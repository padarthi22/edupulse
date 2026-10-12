package com.edupulse.web;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.edupulse.util.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
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
            } catch (Exception e) {
                // ponytail: log at WARN, never let auth-debug spam the log on traffic spikes;
                // upgrade to a per-IP rate-limited debug logger if debugging many tokens.
                log.warn("invalid bearer token from {}: {}", req.getRemoteAddr(), e.getMessage());
            }
        }
        chain.doFilter(req, res);
    }
}
