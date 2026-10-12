package com.edupulse.web;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.edupulse.util.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Reads `Authorization: Bearer <jwt>` and sets the Spring Security principal. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) { this.jwtService = jwtService; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                DecodedJWT jwt = jwtService.verify(header.substring(7));
                String role = jwt.getClaim("role").asString();
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    jwt.getSubject(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // Invalid/expired token — leave unauthenticated; protected endpoints will 401/403.
            }
        }
        chain.doFilter(req, res);
    }
}
