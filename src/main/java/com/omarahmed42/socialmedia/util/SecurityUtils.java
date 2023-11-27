package com.omarahmed42.socialmedia.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.omarahmed42.socialmedia.configuration.security.CustomUserDetails;
import com.omarahmed42.socialmedia.exception.AuthenticationException;
import com.omarahmed42.socialmedia.model.Role;
import com.omarahmed42.socialmedia.model.User;

public class SecurityUtils {
    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityUtils() {

    }

    public static User getAuthenticatedUser() {
        return isAuthenticated() ? ((CustomUserDetails) getPrincipal()).getUser() : null;
    }

    public static Long getAuthenticatedUserId() {
        return isAuthenticated() ? ((CustomUserDetails) getPrincipal()).getUser().getId() : null;
    }

    public static boolean hasGrantedAuthority(String authorityName) {
        return isAuthenticated() && ((CustomUserDetails) getPrincipal())
                .getAuthorities().contains(new SimpleGrantedAuthority(authorityName));
    }

    public static boolean hasRole(String roleName) {
        return isAuthenticated() && ((CustomUserDetails) getPrincipal()).getAuthorities()
                .contains(new SimpleGrantedAuthority(ROLE_PREFIX + roleName));
    }

    public static boolean hasRole(Role role) {
        return isAuthenticated() && ((CustomUserDetails) getPrincipal()).getAuthorities()
                .contains(new SimpleGrantedAuthority(ROLE_PREFIX + role.toString()));
    }

    public static Object getPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null && !(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken);
    }

    public static void throwIfNotAuthenticated() {
        if (!isAuthenticated())
            throw new AuthenticationException("Unauthorized: User unauthenticated");
    }
}
