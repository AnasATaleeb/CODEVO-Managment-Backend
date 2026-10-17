package com.codevo.businessos.auth;
import java.util.*;
import jakarta.validation.constraints.*;
public final class AuthDtos {
    private AuthDtos() {}
    public record Login(@NotBlank @Email @Size(max=255) String email, @NotBlank @Size(max=72) String password) {}
    public record User(UUID id,String email,String name,String role,Set<String> permissions) {}
    public record Session(String accessToken,User user) {}
    public record Issued(Session session,String refreshToken,boolean accepted) {}
}
