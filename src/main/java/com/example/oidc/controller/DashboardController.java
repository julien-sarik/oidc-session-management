package com.example.oidc.controller;

import com.example.oidc.config.SessionStateCaptureFilter;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class DashboardController {

    @Value("${keycloak.check-session-iframe}")
    private String checkSessionIframe;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal OidcUser user,
            HttpSession session,
            Model model) {

        model.addAttribute("username", user.getPreferredUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("name", user.getFullName());
        String sessionState = getSessionState(session, user);
        model.addAttribute("sessionState", sessionState);
        model.addAttribute("checkSessionIframe", checkSessionIframe);
        model.addAttribute("clientId", "oidc-demo-client");

        return "dashboard";
    }

    /**
     * Retrieves the session_state parameter.
     * Per OIDC Session Management spec, session_state is returned in the authorization response.
     */
    private String getSessionState(HttpSession httpSession, OidcUser user) {
        String sessionState = null;

        if (httpSession != null) {
            sessionState = (String) httpSession.getAttribute(SessionStateCaptureFilter.SESSION_STATE_ATTR);
        }

        // Fallback: Try getting from ID token claim (Keycloak and some other OPs include it)
        if (sessionState == null && user != null && user.getIdToken() != null) {
            sessionState = user.getIdToken().getClaim("session_state");
        }

        return sessionState;
    }

    @GetMapping("/api/session-info")
    @ResponseBody
    public Map<String, Object> getSessionInfo(
            @AuthenticationPrincipal OidcUser user,
            HttpSession httpSession) {

        Map<String, Object> sessionInfo = new HashMap<>();

        if (user != null) {
            sessionInfo.put("authenticated", true);
            sessionInfo.put("username", user.getPreferredUsername());
            sessionInfo.put("sessionState", getSessionState(httpSession, user));
            sessionInfo.put("checkSessionIframe", checkSessionIframe);
            sessionInfo.put("clientId", "oidc-demo-client");
        } else {
            sessionInfo.put("authenticated", false);
        }

        return sessionInfo;
    }

    @GetMapping("/logout-oidc")
    public String logoutOidc(@AuthenticationPrincipal OidcUser user) {
        String idToken = user.getIdToken().getTokenValue();
        String logoutUrl = String.format(
            "%s/realms/%s/protocol/openid-connect/logout?id_token_hint=%s&post_logout_redirect_uri=%s",
            authServerUrl,
            realm,
            idToken,
            "http://localhost:8080/"
        );
        return "redirect:" + logoutUrl;
    }
}
