package com.rag.JwtLearn.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JWTService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        log.debug("Extracting username from token");
        String username = extractClaim(token, Claims::getSubject);
        log.debug("Extracted username: {}", username);
        return username;
    }

    public Long extractUserId(String token) {
        log.debug("Extracting user ID from token");
        try {
            Long userId = extractClaim(token, claims -> {
                Object userIdObj = claims.get("userId");
                if (userIdObj instanceof Number) {
                    return ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    return Long.parseLong((String) userIdObj);
                }
                return null;
            });
            log.debug("Extracted user ID: {}", userId);
            return userId;
        } catch (Exception e) {
            log.warn("Could not extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        log.debug("Generating token for user: {}", userDetails.getUsername());
        
        // Add user ID to claims if available
        if (userDetails instanceof com.rag.JwtLearn.user.User) {
            com.rag.JwtLearn.user.User user = (com.rag.JwtLearn.user.User) userDetails;
            extraClaims.put("userId", user.getId());
            log.debug("Added user ID to token: {}", user.getId());
        }
        
        String token = Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        log.debug("Generated token: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
        return token;
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        log.debug("Validating token for user: {}", userDetails.getUsername());
        final String username = extractUsername(token);
        boolean isValid = (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        log.debug("Token validation result: {}", isValid);
        return isValid;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        log.debug("Extracting all claims from token");
        try {
            Claims claims = Jwts
                    .parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.debug("Claims extracted successfully");
            return claims;
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }

    private Key getSigningKey() {
        try {
            // Try to decode as base64 first
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            log.debug("Using base64 decoded secret key");
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // If base64 decoding fails, use the secret as plain text
            log.debug("Using plain text secret key");
            return Keys.hmacShaKeyFor(secretKey.getBytes());
        }
    }
}
