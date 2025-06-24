package com.rag.JwtLearn.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWTService class to handle extraction of info from token
    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            // Params cannot be null
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // "Authorization" is name of header, everything comes after that
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        log.debug("Authorization header: {}", authHeader);
        
        /* If header isn't bearer, JWT filter doesn't auth it, pass it to next filter
           in the chain */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found, passing to next filter");
            filterChain.doFilter(request, response);
            return;
        }
        
        // jwt set to the actual token... comes after "bearer " <Token>
        jwt = authHeader.substring(7);
        log.debug("JWT token extracted: {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "...");
        
        try {
            userEmail = jwtService.extractUsername(jwt);
            log.debug("Extracted username: {}", userEmail);
            
            // Also try to extract user ID for debugging
            Long userId = jwtService.extractUserId(jwt);
            log.debug("Extracted user ID: {}", userId);
            
        } catch (Exception e) {
            log.error("Error extracting username from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check if the user is not already authenticated
        // If not, proceed with authentication
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("User not authenticated, loading user details");
            try {
                // Get UserDetails object from username
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                log.debug("User details loaded for: {}", userEmail);
                
                // Validate the token with the username
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    log.debug("JWT token is valid, setting authentication");
                    // Build an authentication token to update the security context holder
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // Add details of request to auth token
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    // Update security context holder with authentication
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set successfully for user: {}", userEmail);
                } else {
                    log.warn("JWT token is invalid for user: {}", userEmail);
                }
            } catch (Exception e) {
                log.error("Error loading user details for {}: {}", userEmail, e.getMessage(), e);
            }
        } else {
            log.debug("User already authenticated or no username extracted. userEmail: {}, existingAuth: {}", 
                     userEmail, SecurityContextHolder.getContext().getAuthentication());
        }
        
        // Always pass the request to the next filter
        filterChain.doFilter(request, response);
    }
}
