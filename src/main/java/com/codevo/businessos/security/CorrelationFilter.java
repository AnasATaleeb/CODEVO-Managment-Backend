package com.codevo.businessos.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String id=UUID.randomUUID().toString();
        MDC.put("traceId",id); res.setHeader("X-Trace-Id",id);
        try { chain.doFilter(req,res); } finally { MDC.remove("traceId"); }
    }
}
