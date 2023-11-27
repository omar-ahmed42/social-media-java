package com.omarahmed42.socialmedia.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.util.Assert;

import com.omarahmed42.socialmedia.annotation.WithTestUser;

public final class WithTestUserSecurityContextFactory implements WithSecurityContextFactory<WithTestUser> {

    private UserDetailsService userDetailsService;

    @Autowired
    private Environment env;

    @Autowired
    public WithTestUserSecurityContextFactory(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public SecurityContext createSecurityContext(WithTestUser withUser) {
        String email = env.getProperty(withUser.username());

        Assert.hasLength(email, "value() must be non-empty String");
        UserDetails principal = userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(),
                principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
