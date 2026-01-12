# Security Implementation Summary

## Overview
This document provides a comprehensive overview of the OAuth2-based authentication and security implementation for the AI Chatbot application.

## Security Architecture

### Backend Security (Spring Boot)
- **Framework**: Spring Security 6.x
- **Authentication Method**: OAuth2 Resource Server with JWT validation
- **Session Management**: Stateless (no server-side sessions)
- **Token Validation**: JWT tokens validated against OAuth2 provider's public keys (JWKs)

### Frontend Security (React)
- **Authentication Flow**: OAuth2 Authorization Code Flow with PKCE
- **Token Storage**: Managed by react-oauth2-code-pkce library in browser storage
- **Protected Routes**: All application routes require authentication
- **API Protection**: All API calls include JWT Bearer token

## Implementation Details

### Backend Components

#### 1. SecurityConfig (`SecurityConfig.kt`)
- Configures Spring Security filter chain
- Enables OAuth2 Resource Server
- Protects all endpoints except `/actuator/health` and `/error`
- Configures JWT authentication converter with customizable authority claims

**Key Features**:
- Stateless session management
- CSRF disabled (appropriate for stateless APIs)
- Configurable JWT authority claims via application properties

#### 2. CORS Configuration (`CorsConfig.kt`)
- Allows requests from configured frontend origins
- Supports credentials (required for OAuth2)
- Configurable via application properties

#### 3. Application Properties
- OAuth2 issuer URI configuration
- JWK Set URI for token validation
- JWT authority claim customization
- CORS allowed origins

### Frontend Components

#### 1. Authentication Context (`AuthContext.tsx`)
- Wraps the OAuth2 provider from react-oauth2-code-pkce
- Manages authentication state
- Provides login/logout functionality
- Exposes user information and token to the application

#### 2. Login Page (`LoginPage.tsx`)
- Professional login interface
- OAuth2 sign-in button
- Security-focused design

#### 3. Protected App (`App.tsx`)
- Checks authentication state before rendering main UI
- Shows login page if user is not authenticated
- Includes logout button in navigation
- Sets API token when authentication state changes

#### 4. API Service (`api.ts`)
- Centralized API request handling
- Automatically includes JWT Bearer token in all requests
- Configurable auth token via `setAuthToken()` function

## Security Features

### Authentication & Authorization
✅ **JWT Token Validation**: All API requests validate JWT tokens against OAuth2 provider
✅ **Stateless Authentication**: No server-side session storage
✅ **PKCE Flow**: Frontend uses Proof Key for Code Exchange for enhanced security
✅ **Token Expiration**: Tokens expire based on OAuth2 provider settings
✅ **Automatic Token Refresh**: Handled by OAuth2 library

### API Security
✅ **Protected Endpoints**: All API endpoints require valid JWT token (401 if missing/invalid)
✅ **Bearer Token Authentication**: Standard OAuth2 Bearer token scheme
✅ **CORS Protection**: Only configured origins can access the API
✅ **Content-Type Validation**: API expects and returns JSON

### Frontend Security
✅ **Route Protection**: Main application only accessible after authentication
✅ **Automatic Logout**: Users can log out, clearing tokens and state
✅ **Token Injection**: All API calls automatically include authentication token
✅ **Secure Token Storage**: Tokens stored securely by OAuth2 library

## Supported OAuth2 Providers

### Google OAuth2
- **Issuer URI**: `https://accounts.google.com`
- **JWK Set URI**: `https://www.googleapis.com/oauth2/v3/certs`
- **Setup**: Google Cloud Console → APIs & Services → Credentials

### Auth0
- **Issuer URI**: `https://YOUR_DOMAIN.auth0.com/`
- **JWK Set URI**: `https://YOUR_DOMAIN.auth0.com/.well-known/jwks.json`
- **Setup**: Auth0 Dashboard → Applications

### Other Providers
Any OAuth2/OIDC compliant provider can be used by configuring:
- Client ID
- Authorization endpoint
- Token endpoint
- Issuer URI
- JWK Set URI

## Configuration

### Backend Configuration (`application.properties`)
```properties
# OAuth2 Resource Server
spring.security.oauth2.resourceserver.jwt.issuer-uri=<issuer-uri>
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=<jwk-set-uri>

# JWT Authority Claims (customize per provider)
jwt.authorities-claim-name=permissions
jwt.authority-prefix=SCOPE_

# CORS
cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

### Frontend Configuration (`.env`)
```properties
VITE_OAUTH_CLIENT_ID=<your-client-id>
VITE_OAUTH_AUTH_ENDPOINT=<authorization-endpoint>
VITE_OAUTH_TOKEN_ENDPOINT=<token-endpoint>
VITE_OAUTH_REDIRECT_URI=<redirect-uri>
```

## Testing Security

### Manual Testing
1. Start the backend without authentication
2. Try to access API endpoint: `curl http://localhost:8080/api/sessions`
3. **Expected Result**: 401 Unauthorized

### With Authentication
1. Configure OAuth2 provider credentials
2. Start frontend and backend
3. Click "Sign In with OAuth"
4. Complete OAuth2 flow
5. **Expected Result**: Access to application with functional API calls

## Security Best Practices Implemented

1. ✅ **No Secrets in Code**: All sensitive configuration via environment variables
2. ✅ **Secure Token Transmission**: Tokens sent via Authorization header
3. ✅ **HTTPS Ready**: Configuration supports HTTPS for production
4. ✅ **Token Validation**: All tokens validated against provider's public keys
5. ✅ **Minimal Token Exposure**: Tokens only sent to configured API endpoint
6. ✅ **Stateless Architecture**: Scalable and cloud-friendly
7. ✅ **Standard Protocols**: OAuth2 and JWT industry standards
8. ✅ **CORS Protection**: Prevents unauthorized cross-origin requests

## Deployment Considerations

### Production Checklist
- [ ] Use HTTPS for both frontend and backend
- [ ] Set secure OAuth2 redirect URIs
- [ ] Configure production OAuth2 client credentials
- [ ] Set appropriate CORS origins
- [ ] Enable production OAuth2 consent screen
- [ ] Configure proper token expiration times
- [ ] Set up monitoring for authentication failures
- [ ] Implement rate limiting for API endpoints
- [ ] Configure proper logging (excluding tokens)

### Environment Variables
Always use environment variables for:
- OAuth2 client IDs and secrets
- JWT issuer URIs
- CORS allowed origins
- API URLs

Never commit these values to version control!

## Troubleshooting

### 401 Unauthorized Errors
- Verify JWT token is valid and not expired
- Check issuer URI matches OAuth2 provider
- Ensure JWK Set URI is accessible
- Verify token is sent in Authorization header

### CORS Errors
- Add frontend URL to `cors.allowed-origins`
- Restart backend after configuration changes
- Verify frontend URL matches exactly (no trailing slash)

### OAuth2 Redirect Issues
- Ensure redirect URI in code matches provider configuration
- Check for typos in redirect URIs
- Verify OAuth2 provider has correct redirect URIs configured

## References

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [JWT RFC 7519](https://tools.ietf.org/html/rfc7519)
- [PKCE RFC 7636](https://tools.ietf.org/html/rfc7636)
