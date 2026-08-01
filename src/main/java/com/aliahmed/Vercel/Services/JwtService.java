package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.config.AppProperties;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.util.SecretKeyDecoder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * Mints and verifies this application's own access tokens.
 *
 * <p>Deliberately separate from the GitHub token: GitHub proves who the user
 * is once, at login; this token is what the frontend presents on every
 * subsequent call.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final AppProperties.Jwt config;

    public JwtService(AppProperties properties) {
        this.config = properties.getJwt();
        byte[] raw = SecretKeyDecoder.decode(config.getSecret(), "app.jwt.secret", "JWT_SECRET");
        if (raw.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret (env JWT_SECRET) must decode to at least 32 bytes, got "
                            + raw.length + ". Generate one with: openssl rand -base64 32");
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(config.getIssuer())
                .subject(String.valueOf(user.getId()))
                .claim("login", user.getGithubLogin())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(config.getExpiry())))
                .signWith(key)
                .compact();
    }

    /** Returns the user id carried by the token, or empty if it is invalid or expired. */
    public Optional<Long> extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(config.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long expirySeconds() {
        return config.getExpiry().toSeconds();
    }
}
