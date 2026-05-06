package com.kgi.shredder.config;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtil {
    private SecurityContextUtil() {
    }

    public static String currentTrainerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Authenticated trainer is required.");
        }
        return authentication.getName();
    }

    public static String currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Authenticated user is required.");
        }
        Object details = authentication.getDetails();
        if (details instanceof DevUserDetails devUserDetails) {
            return devUserDetails.actorId();
        }
        return currentTrainerId();
    }

    public static List<String> currentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }

    public record DevUserDetails(String actorId, String workspaceTrainerId) {
    }
}
