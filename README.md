# OIDC Session Management Demo

A demonstration project showing how OIDC (OpenID Connect) session management works using Keycloak as the OpenID Provider (OP) and a Spring Boot application as the Relying Party (RP).

## Overview

This project demonstrates **OIDC session monitoring** using the iframe-based session check mechanism defined in the OpenID Connect specification. It shows how a Relying Party can detect when a user's session has changed or ended at the OpenID Provider, even when the change happens in a different browser tab or application.

## How OIDC Session Management Works

### The `check_session_iframe` Mechanism

1. **Session State Parameter**: When a user authenticates, the OP returns a `session_state` value **as a parameter in the authorization response** (the redirect back from the OP). This is captured from the URL query parameters.
2. **Check Session Iframe**: The OP provides an iframe endpoint (`login-status-iframe.html`) that can monitor session status
3. **Postmessage Communication**: The RP periodically sends messages to this iframe with the client ID and session state
4. **Status Responses**: The iframe responds with:
   - `unchanged`: Session is still valid
   - `changed`: Session has changed (user logged out or session expired)
   - `error`: An error occurred during the check

### Sequence Diagram

```
┌─────────┐                    ┌──────────┐                    ┌─────────┐
│   RP    │                    │  iframe  │                    │   OP    │
│ (Page)  │                    │ (hidden) │                    │(Keycloak)│
└────┬────┘                    └────┬─────┘                    └────┬────┘
     │                              │                                │
     │  1. Load iframe              │                                │
     ├─────────────────────────────>│                                │
     │                              │  2. Iframe loads from OP       │
     │                              ├───────────────────────────────>│
     │                              │                                │
     │                              │  3. Send check message         │
     │                              │  "clientId sessionState"       │
     ├                              │───────────────────────────────>│
     │                              │  4. Response: "unchanged"      │
     │                              │  or "changed" or "error"       │
     │                              │<───────────────────────────────┤
```

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                         Pod                                │
│                                                            │
│  ┌──────────────────┐         ┌──────────────────┐       │
│  │   Keycloak       │         │  Spring Boot     │       │
│  │   (Port 8081)    │<───────>│  Application     │       │
│  │                  │  OIDC   │  (Port 8080)     │       │
│  │  - Identity      │         │                  │       │
│  │    Provider      │         │  - OAuth2 Client │       │
│  │  - Session Mgmt  │         │  - Session Mon.  │       │
│  └──────────────────┘         └──────────────────┘       │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

## Quick Start

1. **Build the Spring Boot application Docker image**:
   ```bash
   podman build -t localhost/oidc-session-management:latest .
   ```

2. **Start the pod with Keycloak and the application**:
   ```bash
   podman play kube pods.yml
   ```

3. **Wait for Keycloak to start**, then configure it:
   ```bash
   ./setup-keycloak.sh
   ```

4. **Access the application**:
   - Application: http://localhost:8080
   - Keycloak Admin: http://localhost:8081

5. **Login with demo credentials**:
   - Username: `demo-user`
   - Password: `demo123`


## Testing Session Management

### Test Scenario 1: Normal Session Monitoring

1. Login to the application at http://localhost:8080
2. Observe the "Session Monitoring" section on the dashboard
3. Watch the log entries showing successful session checks every 2 seconds
4. Status should remain "ACTIVE" with regular "unchanged" responses

### Test Scenario 2: Session Change Detection

1. Login to the application in one browser tab
2. In another tab, go to the app and logout from the OP
6. Return to the 1st tab
7. **Observe**: The application automatically detects the session change within 2 seconds
8. You'll see a "SESSION CHANGED" status and receive a prompt to re-authenticate

## Logout Methods Explained

The application demonstrates two logout approaches:

### 1. Local Logout (`/logout`)
- Only invalidates the local application session
- User remains logged in at Keycloak
- Useful for logging out of one application while staying logged in to others

### 2. OIDC Logout (`/logout-oidc`)
- Terminates session at both application and Keycloak (RP-initiated logout)
- Uses the `end_session_endpoint` with `id_token_hint`
- Redirects user through Keycloak logout flow
- Recommended for complete logout

## Cleanup

To stop and remove everything:

```bash
podman play kube --down pods.yml
```