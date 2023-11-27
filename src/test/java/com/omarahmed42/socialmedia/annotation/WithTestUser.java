package com.omarahmed42.socialmedia.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestContext;

import com.omarahmed42.socialmedia.configuration.WithTestUserSecurityContextFactory;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithTestUserSecurityContextFactory.class)
@WithUserDetails
public @interface WithTestUser {
    /**
     * The username from the properties file to look up in the
     * {@link UserDetailsService}
     * 
     * @return
     */
    String username() default "";

    /**
     * Determines when the {@link SecurityContext} is setup. The default is before
     * {@link TestExecutionEvent#TEST_METHOD} which occurs during
     * {@link org.springframework.test.context.TestExecutionListener#beforeTestMethod(TestContext)}
     * 
     * @return the {@link TestExecutionEvent} to initialize before
     * @since 5.1
     */
    @AliasFor(annotation = WithSecurityContext.class)
    TestExecutionEvent setupBefore() default TestExecutionEvent.TEST_METHOD;
}