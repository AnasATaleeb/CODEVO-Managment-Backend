package com.codevo.businessos.auth;

import com.codevo.businessos.common.DomainException;
import com.codevo.businessos.config.AppProperties;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    private final AppProperties props;
    public AuthController(AuthService service,AppProperties props) { this.service=service;this.props=props; }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"headerName",token.getHeaderName()); }
    @PostMapping("/login") public AuthDtos.Session login(@Valid @RequestBody AuthDtos.Login request,HttpServletResponse response) {
        var issued=service.login(request);cookie(response,issued.refreshToken());return issued.session();
    }
    @PostMapping("/refresh") public AuthDtos.Session refresh(@CookieValue(value="codevo_refresh",required=false) String raw,HttpServletResponse response) {
        var issued=service.refresh(raw);
        if(!issued.accepted()) { cookie(response,null);throw new DomainException(401,"SESSION_EXPIRED","Session expired; please sign in"); }
        cookie(response,issued.refreshToken());return issued.session();
    }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(value="codevo_refresh",required=false) String raw,HttpServletResponse response) { service.logout(raw);cookie(response,null); }
    @GetMapping("/me") public AuthDtos.User me(Authentication auth) { return service.user(UUID.fromString(auth.getName())); }
    private void cookie(HttpServletResponse response,String raw) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("codevo_refresh",raw==null?"":raw)
            .httpOnly(true).secure(props.secureCookies()).sameSite("Strict").path("/api/v1/auth")
            .maxAge(raw==null?java.time.Duration.ZERO:props.refreshTokenTtl()).build().toString());
        response.setHeader("Cache-Control","no-store");
    }
}
