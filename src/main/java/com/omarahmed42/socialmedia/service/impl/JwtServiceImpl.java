package com.omarahmed42.socialmedia.service.impl;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.builder.Token;
import com.omarahmed42.socialmedia.dto.response.Jwt;
import com.omarahmed42.socialmedia.dto.response.JwtResponse;
import com.omarahmed42.socialmedia.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImpl implements JwtService {

    @Value(value = "${token.signing.key}")
    private String jwtSigningKey;

    @Override
    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Jwt generateToken(Token token) {
        JwtBuilder jwtBuilder = Jwts.builder();
        if (nonEmpty(token.getId()))
            jwtBuilder.id(token.getId());
        if (nonEmpty(token.getIssuer()))
            jwtBuilder.issuer(token.getIssuer());
        if (nonEmpty(token.getSubject()))
            jwtBuilder.subject(token.getSubject());
        if (nonEmpty(token.getAudience()))
            jwtBuilder.audience().add(token.getAudience());
        if (nonEmpty(token.getExpiration()))
            jwtBuilder.expiration(token.getExpiration());
        if (nonEmpty(token.getIssuedAt()))
            jwtBuilder.issuedAt(token.getIssuedAt());
        if (nonEmpty(token.getNotBefore()))
            jwtBuilder.notBefore(token.getNotBefore());
        if (nonEmpty(token.getExtra()))
            jwtBuilder.claims(token.getExtra());

        return new Jwt(jwtBuilder.signWith(getSigningKey()).compact());
    }

    private boolean nonEmpty(Object obj) {
        return ObjectUtils.isNotEmpty(obj);
    }

    @Override
    public Token parse(String jwt) {
        Claims payload = extractAllClaims(jwt);

        return Token.builder().id(payload.getId()).issuer(payload.getIssuer()).subject(payload.getSubject())
                .audience(StringUtils.join(payload.getAudience(), ", ")).expiration(payload.getExpiration())
                .notBefore(payload.getNotBefore()).issuedAt(payload.getIssuedAt()).build();
    }

    @Override
    public JwtResponse generateTokens(String subject) {
        Token builtAccessToken = Token.builder().subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + Duration.ofMinutes(24L).toMillis())).build();
        Jwt accessToken = generateToken(builtAccessToken);

        Token builtRefreshToken = Token.builder().id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + Duration.ofDays(15L).toMillis())).build();

        Jwt refreshToken = generateToken(builtRefreshToken);

        return new JwtResponse(accessToken.token(), refreshToken.token());
    }

}
