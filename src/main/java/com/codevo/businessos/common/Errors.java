package com.codevo.businessos.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashMap;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class Errors {
    private static final Logger LOG = LoggerFactory.getLogger(Errors.class);
    public static ApiError error(int status, String code, String message, HttpServletRequest req, Map<String,String> fields) {
        return new ApiError(Instant.now(), status, code, message, req.getRequestURI(), fields, MDC.get("traceId"));
    }
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException e, HttpServletRequest r) {
        return ResponseEntity.status(e.getStatus()).body(error(e.getStatus(),e.getCode(),e.getMessage(),r,Map.of()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String,String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(),f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(error(400,"VALIDATION_ERROR","Please check the highlighted fields",r,fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> invalid(Exception e, HttpServletRequest r) {
        return ResponseEntity.badRequest().body(error(400,"VALIDATION_ERROR","Invalid request fields, dates or identifiers",r,Map.of()));
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(HttpServletRequest r) {
        return ResponseEntity.status(403).body(error(403,"FORBIDDEN","You do not have permission for this action",r,Map.of()));
    }
    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class, CannotAcquireLockException.class})
    ResponseEntity<ApiError> conflict(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(409).body(error(409,"CONFLICT","Record changed or violates a relationship/uniqueness rule; refresh and retry",r,Map.of()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> upload(HttpServletRequest r) {
        return ResponseEntity.status(413).body(error(413,"FILE_TOO_LARGE","Maximum upload size is 20 MB",r,Map.of()));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest r) {
        LOG.error("Request failed: {}",e.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(error(500,"INTERNAL_ERROR","Unable to complete the request",r,Map.of()));
    }
}
