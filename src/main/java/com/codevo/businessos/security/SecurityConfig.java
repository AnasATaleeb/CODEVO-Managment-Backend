package com.codevo.businessos.security;

import com.codevo.businessos.common.Errors;
import com.codevo.businessos.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    @Bean JwtEncoder encoder(AppProperties p) { return new NimbusJwtEncoder(new ImmutableSecret<>(p.jwtSecret().getBytes(StandardCharsets.UTF_8))); }
    @Bean JwtDecoder decoder(AppProperties p) {
        var decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(p.jwtSecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256")).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("codevo-business-os"),
            new JwtClaimValidator<List<String>>("aud",aud->aud!=null && aud.contains("codevo-web"))));
        return decoder;
    }
    @Bean SecurityFilterChain chain(HttpSecurity http, AppProperties p, JdbcTemplate jdbc, ObjectMapper json, org.springframework.core.env.Environment environment) throws Exception {
        var csrf=new CookieCsrfTokenRepository();
        csrf.setCookieCustomizer(c->c.path("/").sameSite("Strict").secure(p.secureCookies()).httpOnly(true));
        var handler=new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName("_csrf");
        http.cors(c->c.configurationSource(cors(p)))
            .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(handler)
                .sessionAuthenticationStrategy((authentication,request,response)->{})
                .requireCsrfProtectionMatcher(r->r.getMethod().equals("POST") && r.getRequestURI().startsWith("/api/v1/auth/")))
            .authorizeHttpRequests(c->c
                .requestMatchers("/api/v1/auth/csrf","/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout","/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").access((authentication,context)->new org.springframework.security.authorization.AuthorizationDecision(environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev")) || authentication.get().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_OWNER"))))
                .requestMatchers("/api/v1/**").authenticated().anyRequest().denyAll())
            .oauth2ResourceServer(c->c.jwt(j->j.jwtAuthenticationConverter(jwt->{
                UUID session, user;
                try { session=UUID.fromString(jwt.getClaimAsString("sid"));user=UUID.fromString(jwt.getSubject()); }
                catch(Exception e) { throw new BadCredentialsException("Invalid session"); }
                String role=jdbc.query("select u.role from auth_sessions s join app_users u on u.id=s.user_id where s.id=? and u.id=? and s.revoked_at is null and s.expires_at>now() and u.active=true",
                    rs->rs.next()?rs.getString(1):null,session,user);
                if(role==null) throw new BadCredentialsException("Session revoked");
                List<SimpleGrantedAuthority> authorities=new ArrayList<>();
                Permissions.forRole(role).forEach(a->authorities.add(new SimpleGrantedAuthority(a)));
                authorities.add(new SimpleGrantedAuthority("ROLE_"+role));
                return new UsernamePasswordAuthenticationToken(user.toString(),null,authorities);
            })).authenticationEntryPoint((req,res,e)->{
                res.setStatus(401);res.setContentType("application/json");
                json.writeValue(res.getOutputStream(),Errors.error(401,"UNAUTHENTICATED","Please sign in again",req,Map.of()));
            }))
            .exceptionHandling(c->c.authenticationEntryPoint((req,res,e)->{
                res.setStatus(401);res.setContentType("application/json");
                json.writeValue(res.getOutputStream(),Errors.error(401,"UNAUTHENTICATED","Please sign in",req,Map.of()));
            }).accessDeniedHandler((req,res,e)->{
                res.setStatus(403);res.setContentType("application/json");
                json.writeValue(res.getOutputStream(),Errors.error(403,e instanceof CsrfException?"CSRF_INVALID":"FORBIDDEN",
                    e instanceof CsrfException?"Refresh the form and try again":"You do not have permission",req,Map.of()));
            }))
            .headers(c->c.contentSecurityPolicy(v->v.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'")))
            .addFilterBefore(new CorrelationFilter(),CsrfFilter.class)
            .addFilterBefore(new AuthRateLimitFilter(),CsrfFilter.class);
        return http.build();
    }
    private CorsConfigurationSource cors(AppProperties p) {
        var config=new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(p.frontendUrl().split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","X-XSRF-TOKEN","X-CSRF-TOKEN"));
        config.setExposedHeaders(List.of("X-Trace-Id","Content-Disposition"));
        config.setAllowCredentials(true);
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",config);return source;
    }
}
