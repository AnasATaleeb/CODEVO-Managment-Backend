package com.codevo.businessos.auth;

import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import com.codevo.businessos.common.*;
import com.codevo.businessos.config.AppProperties;
import com.codevo.businessos.security.Permissions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final AppProperties props;
    private final Clock clock;
    private final SecureRandom random=new SecureRandom();
    private final String dummyHash;
    public AuthService(JdbcTemplate jdbc,PasswordEncoder passwords,JwtEncoder encoder,AppProperties props,Clock clock) {
        this.jdbc=jdbc;this.passwords=passwords;this.encoder=encoder;this.props=props;this.clock=clock;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    @Transactional
    public AuthDtos.Issued login(AuthDtos.Login request) {
        var user=jdbc.query("select id,email,name,role,password_hash,active from app_users where email=?",
            rs->rs.next()?Map.of("id",com.codevo.businessos.common.SqlIds.uuid(rs,"id"),"email",rs.getString("email"),"name",rs.getString("name"),
                "role",rs.getString("role"),"password",rs.getString("password_hash"),"active",rs.getBoolean("active")):null,request.email().toLowerCase(Locale.ROOT).trim());
        boolean matches=passwords.matches(request.password(),user==null?dummyHash:(String)user.get("password"));
        if(user==null || !matches || !Boolean.TRUE.equals(user.get("active"))) throw new DomainException(401,"INVALID_CREDENTIALS","Email or password is incorrect");
        UUID session=UUID.randomUUID();
        jdbc.update("insert into auth_sessions(id,user_id,expires_at) values(?,?,?)",session,user.get("id"),java.sql.Timestamp.from(clock.instant().plus(props.refreshTokenTtl())));
        return issue(session,(UUID)user.get("id"));
    }
    @Transactional
    public AuthDtos.Issued refresh(String raw) {
        if(raw==null || raw.length()>256) return new AuthDtos.Issued(null,null,false);
        var token=jdbc.query("select t.id,t.session_id,t.used_at,t.expires_at,s.user_id,s.revoked_at from refresh_tokens t join auth_sessions s on s.id=t.session_id where t.token_hash=? for update",
            rs->rs.next()?new Refresh(com.codevo.businessos.common.SqlIds.uuid(rs,1),com.codevo.businessos.common.SqlIds.uuid(rs,2),rs.getTimestamp(3)!=null,rs.getTimestamp(4).toInstant(),com.codevo.businessos.common.SqlIds.uuid(rs,5),rs.getTimestamp(6)!=null):null,hash(raw));
        if(token==null) return new AuthDtos.Issued(null,null,false);
        if(token.used()) {
            jdbc.update("update auth_sessions set revoked_at=coalesce(revoked_at,?) where id=?",java.sql.Timestamp.from(clock.instant()),token.sessionId());
            return new AuthDtos.Issued(null,null,false);
        }
        if(token.revoked() || !token.expiresAt().isAfter(clock.instant()) || !Boolean.TRUE.equals(jdbc.queryForObject("select active from app_users where id=?",Boolean.class,token.userId())))
            return new AuthDtos.Issued(null,null,false);
        jdbc.update("update refresh_tokens set used_at=? where id=?",java.sql.Timestamp.from(clock.instant()),token.id());
        return issue(token.sessionId(),token.userId());
    }
    @Transactional
    public void logout(String raw) {
        if(raw!=null && raw.length()<=256) jdbc.update("update auth_sessions set revoked_at=coalesce(revoked_at,?) where id in (select session_id from refresh_tokens where token_hash=?)",java.sql.Timestamp.from(clock.instant()),hash(raw));
    }
    @Transactional(readOnly=true)
    public AuthDtos.User user(UUID id) {
        return jdbc.query("select id,email,name,role from app_users where id=? and active=true",
            rs->{ if(!rs.next()) throw DomainException.missing("User");return new AuthDtos.User(id,rs.getString(2),rs.getString(3),rs.getString(4),Permissions.forRole(rs.getString(4))); },id);
    }
    private AuthDtos.Issued issue(UUID session,UUID userId) {
        var user=user(userId);
        byte[] bytes=new byte[48];random.nextBytes(bytes);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry=jdbc.queryForObject("select expires_at from auth_sessions where id=?",java.sql.Timestamp.class,session).toInstant();
        jdbc.update("insert into refresh_tokens(id,session_id,token_hash,expires_at) values(?,?,?,?)",UUID.randomUUID(),session,hash(raw),java.sql.Timestamp.from(expiry));
        var claims=JwtClaimsSet.builder().issuer("codevo-business-os").subject(userId.toString()).audience(List.of("codevo-web"))
            .issuedAt(clock.instant()).expiresAt(clock.instant().plus(props.accessTokenTtl())).claim("sid",session.toString()).build();
        String access=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new AuthDtos.Issued(new AuthDtos.Session(access,user),raw,true);
    }
    public static String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable",e); }
    }
    private record Refresh(UUID id,UUID sessionId,boolean used,Instant expiresAt,UUID userId,boolean revoked) {}
}
