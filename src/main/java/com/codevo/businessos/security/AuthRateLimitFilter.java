package com.codevo.businessos.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.filter.OncePerRequestFilter;

/** A bounded single-instance limiter; use a shared edge limiter for multiple instances. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Window(long minute, int count) {}
    private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(req.getMethod().equals("POST") && req.getRequestURI().startsWith("/api/v1/auth/")) {
            long minute=System.currentTimeMillis()/60000;
            windows.entrySet().removeIf(e->e.getValue().minute()<minute);
            if(windows.size()>10000) { reject(req,res); return; }
            Window w=windows.compute(req.getRemoteAddr()+":"+req.getRequestURI(),(key,old)->old==null || old.minute()!=minute?new Window(minute,1):new Window(minute,old.count()+1));
            int limit=req.getRequestURI().endsWith("/login")?30:120;
            if(w.count()>limit) { reject(req,res); return; }
        }
        chain.doFilter(req,res);
    }
    private void reject(HttpServletRequest req,HttpServletResponse res) throws IOException {
        res.setStatus(429);res.setHeader("Retry-After","60");res.setContentType("application/json");
        res.getWriter().write("{\"status\":429,\"code\":\"RATE_LIMITED\",\"message\":\"Too many authentication requests. Try again in one minute.\",\"fieldErrors\":{}}");
    }
}
