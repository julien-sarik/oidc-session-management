package com.example.oidc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that captures the session_state parameter from the OAuth2/OIDC authorization response.
 *
 * Per OIDC Session Management spec, session_state is returned as a URL parameter in the
 * authorization response (the redirect from the OP back to the RP).
 *
 * This filter intercepts requests to the OAuth2 callback endpoint (/login/oauth2/code/*)
 * and captures the session_state parameter.
 */
public class SessionStateCaptureFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SessionStateCaptureFilter.class);

    public static final String SESSION_STATE_ATTR = "oidc.session_state";
    private static final String OAUTH2_CALLBACK_PATH = "/login/oauth2/code/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Check if this is an OAuth2 authorization response callback
        if (requestPath.startsWith(OAUTH2_CALLBACK_PATH)) {
            logger.info("OAuth2 callback detected! Full URL: {}", request.getRequestURL() + "?" + request.getQueryString());

            String sessionState = request.getParameter("session_state");

            if (sessionState != null && !sessionState.isEmpty()) {
                HttpSession session = request.getSession();
                session.setAttribute(SESSION_STATE_ATTR, sessionState);

                logger.info("✓ Captured session_state from authorization response: {}", sessionState);
            } else {
                logger.warn("⚠ OAuth2 callback received but session_state parameter is missing or empty");
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
