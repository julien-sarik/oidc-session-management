#!/bin/bash

# Script to setup Keycloak realm and client for OIDC demo
# This script should be run after Keycloak is up and running

KEYCLOAK_URL="http://localhost:8081"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
REALM_FILE="keycloak-realm.json"

echo "Waiting for Keycloak to be ready..."
until $(curl --output /dev/null --silent --head --fail ${KEYCLOAK_URL}); do
    printf '.'
    sleep 5
done
echo ""
echo "Keycloak is ready!"

# Get admin token
echo "Authenticating as admin..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
    echo "Failed to get admin token"
    exit 1
fi

echo "Admin token obtained successfully"

# Check if realm already exists
REALM_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/demo")

if [ "$REALM_EXISTS" = "200" ]; then
    echo "Realm 'demo' already exists. Deleting it..."
    curl -s -X DELETE \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      "${KEYCLOAK_URL}/admin/realms/demo"
    echo "Realm deleted"
fi

# Import realm
echo "Creating realm from ${REALM_FILE}..."
RESPONSE=$(curl -s -w "%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d @${REALM_FILE})

HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "201" ]; then
    echo "✓ Realm created successfully!"
    echo ""
    echo "======================================"
    echo "Keycloak Configuration Complete!"
    echo "======================================"
    echo ""
    echo "Keycloak Admin Console: ${KEYCLOAK_URL}"
    echo "  Username: ${ADMIN_USER}"
    echo "  Password: ${ADMIN_PASSWORD}"
    echo ""
    echo "Demo User Credentials:"
    echo "  Username: demo-user"
    echo "  Password: demo123"
    echo ""
    echo "Client ID: oidc-demo-client"
    echo "Client Secret: your-client-secret-here"
    echo ""
    echo "You can now start the Spring Boot application!"
    echo "======================================"
else
    echo "✗ Failed to create realm. HTTP code: $HTTP_CODE"
    exit 1
fi
