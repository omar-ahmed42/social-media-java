package com.omarahmed42.socialmedia.builder;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Token implements Serializable {
    @JsonProperty("jti")
    private String id;

    @JsonProperty("iss")
    private String issuer;

    @JsonProperty("sub")
    private String subject;

    @JsonProperty("username")
    private String username;

    @JsonProperty("aud")
    private String audience;

    @JsonProperty("exp")
    private Date expiration;

    @JsonProperty("nbf")
    private Date notBefore;

    @JsonProperty("iat")
    private Date issuedAt;

    private Map<String, Object> extra;
}
