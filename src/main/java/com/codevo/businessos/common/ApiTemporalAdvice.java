package com.codevo.businessos.common;

import java.time.*;
import java.util.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** MySQL DATETIME columns contain UTC instants. Never emit an ambiguous offset-free SQL timestamp. */
@RestControllerAdvice
public class ApiTemporalAdvice implements ResponseBodyAdvice<Object> {
    @Override public boolean supports(MethodParameter parameter,Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public Object beforeBodyWrite(Object body,MethodParameter parameter,MediaType type,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest request,ServerHttpResponse response) {
        return normalize(body);
    }
    private Object normalize(Object value) {
        if(value instanceof LocalDateTime date) return date.toInstant(ZoneOffset.UTC);
        if(value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        if(value instanceof java.sql.Date date) return date.toLocalDate();
        if(value instanceof Map<?,?> map) {
            Map<Object,Object> result=new LinkedHashMap<>();map.forEach((key,item)->result.put(key,normalize(item)));return result;
        }
        if(value instanceof List<?> list) return list.stream().map(this::normalize).toList();
        if(value instanceof PageResponse<?> page) return new PageResponse<>(page.content().stream().map(this::normalize).toList(),page.page(),page.size(),page.totalElements(),page.totalPages());
        return value;
    }
}
