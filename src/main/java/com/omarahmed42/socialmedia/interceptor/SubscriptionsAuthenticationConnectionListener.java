package com.omarahmed42.socialmedia.interceptor;

import java.util.Map;

import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.omarahmed42.socialmedia.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionsAuthenticationConnectionListener implements WebSocketGraphQlInterceptor {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	public Mono<Object> handleConnectionInitialization(
			WebSocketSessionInfo sessionInfo, Map<String, Object> connectionInitPayload) {

		String jwt = (String) connectionInitPayload.get("Authorization");

		if (jwt != null && jwt.startsWith(BEARER_PREFIX)) {
			log.info("Authentication token: {}", jwt);
			val accessToken = jwt.substring(BEARER_PREFIX.length());
			if (StringUtils.hasText(accessToken)) {
				UserDetails userDetails = userDetailsService
						.loadUserByUsername(jwtService.extractUserName(accessToken));
				Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
						userDetails.getAuthorities());

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}

		return Mono.empty();
	}
}