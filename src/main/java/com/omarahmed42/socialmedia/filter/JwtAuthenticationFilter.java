package com.omarahmed42.socialmedia.filter;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.omarahmed42.socialmedia.exception.AccessTokenExpiredException;
import com.omarahmed42.socialmedia.exception.UnauthorizedException;
import com.omarahmed42.socialmedia.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.resolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("Request to URL {} with request id {}", request.getRequestURI(), request.getRequestId());
        if ("/actuator/prometheus".equals(request.getRequestURI()) || isAuthEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !StringUtils.startsWith(authHeader, "Bearer ")) {
            log.error("No Bearer token present");
            doFilter(request, response, filterChain);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractUserName(jwt);
            final Long userId = jwtService.extractSubject(jwt);
            if (userId != null && userId > 0 && StringUtils.isNotEmpty(userEmail)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt)) {
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                            null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    context.setAuthentication(authToken);
                    SecurityContextHolder.setContext(context);
                    filterChain.doFilter(request, response);
                }
            } else {
                resolver.resolveException(request, response, null,
                        new UnauthorizedException("Unauthorized: User unauthenticated"));
            }
        } catch (ExpiredJwtException e) {
            log.error("Token expired: {}", e);
            resolver.resolveException(request, response, null, new AccessTokenExpiredException());
        }

    }

    private boolean isAuthEndpoint(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return "/api/v1/auth/login".equals(requestURI)
                || "/api/v1/auth/signup".equals(requestURI)
                || "/api/v1/auth/tokens/refresh".equals(requestURI);
    }

}
