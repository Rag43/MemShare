package com.rag.JwtLearn.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
        /* If header isn't bearer, JWT filter doesn't auth it, pass it to next filter
           in the chain */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // jwt set to the actual token... comes after "bearer " <Token>
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt);
        // Check if the user is not already authenticated
        // If not, proceed with authentication
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Get UserDetails object from username
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            // Validate the token with the username
            if (jwtService.isTokenValid(jwt, userDetails)) {
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
            }
            // Pass the request to the next filter
            filterChain.doFilter(request, response);
        }
    }
}
