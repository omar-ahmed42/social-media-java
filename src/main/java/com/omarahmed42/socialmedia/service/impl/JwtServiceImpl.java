package com.omarahmed42.socialmedia.service.impl;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.omarahmed42.socialmedia.builder.Token;
import com.omarahmed42.socialmedia.dto.response.Jwt;
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
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder().claims(extraClaims).subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))
                .signWith(getSigningKey()).compact();
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
    public String generateToken(UserDetails userDetails, Long expiration) {
        return generateToken(new HashMap<>(), userDetails, expiration);
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, Long expiration) {
        return Jwts.builder().claims(extraClaims).subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey()).compact();
    }

    @Override
    public Jwt generateToken(Token token) {
        JwtBuilder jwtBuilder = Jwts.builder();
        if (nonEmpty(token.getId())) jwtBuilder.id(token.getId());
        if (nonEmpty(token.getIssuer())) jwtBuilder.issuer(token.getIssuer());
        if (nonEmpty(token.getSubject())) jwtBuilder.subject(token.getSubject());
        if (nonEmpty(token.getAudience())) jwtBuilder.audience().add(token.getAudience());
        if (nonEmpty(token.getExpiration())) jwtBuilder.expiration(token.getExpiration());
        if (nonEmpty(token.getIssuedAt())) jwtBuilder.issuedAt(token.getIssuedAt());
        if (nonEmpty(token.getNotBefore())) jwtBuilder.notBefore(token.getNotBefore());
        if (nonEmpty(token.getExtra())) jwtBuilder.claims(token.getExtra());

        return new Jwt(jwtBuilder.signWith(getSigningKey()).compact());
    }

    public boolean nonEmpty(Object obj) {
        return ObjectUtils.isNotEmpty(obj);
    }

}
