package com.edupulse.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.edupulse.config.EduPulseProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class JwtService {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expiresSeconds;

    public JwtService(EduPulseProperties props) {
        String secret = props.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 chars");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expiresSeconds = Duration.ofDays(7).toSeconds();
    }

    public String issue(String userId, String role, String name) {
        Instant now = Instant.now();
        return JWT.create()
            .withSubject(userId)
            .withClaim("role", role)
            .withClaim("name", name)
            .withIssuedAt(now)
            .withExpiresAt(now.plusSeconds(expiresSeconds))
            .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}
