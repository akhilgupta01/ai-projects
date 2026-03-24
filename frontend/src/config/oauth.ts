// OAuth2 configuration for Google OAuth
// For production, these values should come from environment variables
export const oauth2Config = {
  authEnabled: import.meta.env.VITE_AUTH_ENABLED === "true",
  clientId: import.meta.env.VITE_OAUTH_CLIENT_ID || "YOUR_GOOGLE_CLIENT_ID",
  clientSecret: import.meta.env.VITE_OAUTH_CLIENT_SECRET,
  authorizationEndpoint:
    import.meta.env.VITE_OAUTH_AUTH_ENDPOINT ||
    "https://accounts.google.com/o/oauth2/v2/auth",
  tokenEndpoint:
    import.meta.env.VITE_OAUTH_TOKEN_ENDPOINT ||
    "https://oauth2.googleapis.com/token",
  redirectUri:
    import.meta.env.VITE_OAUTH_REDIRECT_URI || "http://localhost:5173",
  scope: "openid profile email",
  // Optional: Add your token endpoint for refresh
  refreshTokenEndpoint:
    import.meta.env.VITE_OAUTH_TOKEN_ENDPOINT ||
    "https://oauth2.googleapis.com/token",
};
