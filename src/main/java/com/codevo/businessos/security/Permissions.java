package com.codevo.businessos.security;

import java.util.*;

public final class Permissions {
    private Permissions() {}
    public static Set<String> forRole(String role) {
        return switch(role) {
            case "OWNER" -> Set.of("CRM_READ","CRM_WRITE","WORK_READ","WORK_WRITE","CATALOG_WRITE","FINANCE_READ","FINANCE_WRITE","SETTINGS_WRITE","USERS_WRITE");
            case "ADMIN" -> Set.of("CRM_READ","CRM_WRITE","WORK_READ","WORK_WRITE","CATALOG_WRITE","FINANCE_READ","FINANCE_WRITE");
            case "STAFF" -> Set.of("CRM_READ","CRM_WRITE","WORK_READ","WORK_WRITE");
            case "ACCOUNTANT" -> Set.of("CRM_READ","WORK_READ","FINANCE_READ","FINANCE_WRITE");
            default -> Set.of();
        };
    }
    public static boolean finance(org.springframework.security.core.Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("FINANCE_READ"));
    }
    public static boolean currentHas(String permission) {
        var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth!=null && auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals(permission));
    }
    public static void requireFinancialWrite() {
        if(!currentHas("FINANCE_WRITE")) throw new org.springframework.security.access.AccessDeniedException("Financial write permission required");
    }
}
