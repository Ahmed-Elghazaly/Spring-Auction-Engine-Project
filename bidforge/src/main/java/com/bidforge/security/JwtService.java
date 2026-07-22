package com.bidforge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;


// Creates and validates JWTs

@Service
public class JwtService {

    // secret key for calculating signature, from application.properties (bidforge.jwt.secret).
    private final SecretKey key;

    // Token lifetime in milliseconds, from application.properties (bidforge.jwt.expiration-ms).
    private final long expirationMs;

    public JwtService(@Value("${bidforge.jwt.secret}") String secret, @Value("${bidforge.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }


    // Issues a token for a successfully authenticated user
    // token contains smth like: subject = username, "roles" =  ["ROLE_USER"], issued-at and expiration timestamps
    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return Jwts.builder().subject(userDetails.getUsername()).claim("roles", roles).issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expirationMs))).signWith(key).compact();
    }


    // Verifies the signature and expiry, then returns the username stored in the token
    // Throws exception if the token is invalid in any way
    // the caller JwtAuthFilter treats that as "not authenticated".
    public String extractUsername(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
