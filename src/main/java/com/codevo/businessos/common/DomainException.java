package com.codevo.businessos.common;

import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {
    private final int status;
    private final String code;
    public DomainException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public static DomainException missing(String name) { return new DomainException(404, "NOT_FOUND", name + " not found"); }
    public static DomainException conflict(String message) { return new DomainException(409, "CONFLICT", message); }
    public static DomainException invalid(String message) { return new DomainException(400, "VALIDATION_ERROR", message); }
}
