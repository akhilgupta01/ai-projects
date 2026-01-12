# AI Operational Support ChatBot

An AI-assisted operational support chatbot application with a React/TypeScript frontend and Kotlin/SpringBoot backend, secured with OAuth2 authentication.

## Project Structure

```
ai-chatbot/
├── backend/          # Kotlin/SpringBoot backend
└── frontend/         # React/TypeScript frontend
```

## Security

This application is secured with OAuth2 authentication using JWT tokens:
- **Backend**: Spring Security OAuth2 Resource Server validates JWT tokens
- **Frontend**: OAuth2 PKCE flow for secure authentication
- **APIs**: All API endpoints require valid JWT authentication tokens

## Backend (Kotlin/SpringBoot)

### Features
- REST APIs for chat session management
- Create, list, get, and delete chat sessions
- Send messages and receive AI assistant responses
- In-memory storage for chat sessions
- **OAuth2 Resource Server for JWT token validation**
- **Secured API endpoints**
- CORS configured for frontend communication

### API Endpoints

All endpoints require a valid JWT token in the `Authorization` header:
```
Authorization: Bearer <your-jwt-token>
```

- `POST /api/sessions` - Create a new chat session
- `GET /api/sessions` - Get all chat sessions
- `GET /api/sessions/{sessionId}` - Get a specific session with messages
- `POST /api/sessions/{sessionId}/messages` - Send a message and get bot response
- `DELETE /api/sessions/{sessionId}` - Delete a chat session

### Backend Configuration

Create or update `application.properties` in `backend/src/main/resources/`:

```properties
server.port=8080
spring.application.name=ai-chatbot-backend

# OAuth2 Resource Server Configuration
# For Google OAuth2:
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://accounts.google.com
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://www.googleapis.com/oauth2/v3/certs

# CORS allowed origins (comma-separated)
cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

#### Using Auth0 (Alternative)

If using Auth0 instead of Google OAuth:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://YOUR_DOMAIN.auth0.com/
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://YOUR_DOMAIN.auth0.com/.well-known/jwks.json
```

### Running the Backend

```bash
cd backend
./gradlew bootRun
```

The backend will start on `http://localhost:8080`.

### Building for Production

```bash
cd backend
./gradlew build
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

## Frontend (React/TypeScript)

### Features
- Chat page with message display and input
- Sidebar with session list and "New Chat" button
- Session switching and deletion
- Clean, modern UI with Tailwind CSS
- Real-time message display
- **OAuth2 Authentication with PKCE flow**
- **Login/Logout functionality**
- **Protected routes and API calls**

### Frontend Configuration

Create a `.env` file in the frontend directory:

```properties
VITE_API_URL=http://localhost:8080

# OAuth2 Configuration
# For Google OAuth: Get these from https://console.cloud.google.com/apis/credentials
VITE_OAUTH_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID
VITE_OAUTH_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
VITE_OAUTH_AUTH_ENDPOINT=https://accounts.google.com/o/oauth2/v2/auth
VITE_OAUTH_TOKEN_ENDPOINT=https://oauth2.googleapis.com/token
VITE_OAUTH_REDIRECT_URI=http://localhost:5173
```

#### Setting up Google OAuth2

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google+ API
4. Go to "APIs & Services" > "Credentials"
5. Click "Create Credentials" > "OAuth 2.0 Client ID"
6. Configure the consent screen if not already done
7. Select "Web application" as the application type
8. Add authorized redirect URIs:
   - `http://localhost:5173` (for development)
   - Your production URL
9. Copy the **Client ID** and **Client Secret** and update your `.env` file

**Note:** The Client Secret is required for the OAuth token exchange. Keep it secure and never commit it to version control.

#### Using Auth0 (Alternative)

If using Auth0:

```properties
VITE_OAUTH_CLIENT_ID=YOUR_AUTH0_CLIENT_ID
VITE_OAUTH_CLIENT_SECRET=YOUR_AUTH0_CLIENT_SECRET
VITE_OAUTH_AUTH_ENDPOINT=https://YOUR_DOMAIN.auth0.com/authorize
VITE_OAUTH_TOKEN_ENDPOINT=https://YOUR_DOMAIN.auth0.com/oauth/token
VITE_OAUTH_REDIRECT_URI=http://localhost:5173
```

### Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:5173`.

### Building for Production

```bash
cd frontend
npm install
npm run build
```

The build output will be in the `dist` directory.

## Complete Setup Guide

### Prerequisites
- Java 17 or higher
- Node.js 18 or higher
- OAuth2 provider credentials (Google, Auth0, etc.)

### Step-by-Step Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ai-chatbot
   ```

2. **Set up OAuth2 Provider**
   - Follow the instructions above for Google OAuth or Auth0
   - Note down your Client ID and redirect URI

3. **Configure Backend**
   - Update `backend/src/main/resources/application.properties` with your OAuth2 issuer URI
   - Ensure CORS origins include your frontend URL

4. **Configure Frontend**
   - Create `.env` file in `frontend/` directory
   - Add your OAuth2 Client ID and endpoints
   - Update API URL if needed

5. **Start Backend**
   ```bash
   cd backend
   ./gradlew bootRun
   ```

6. **Start Frontend** (in a new terminal)
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

7. **Access the Application**
   - Open `http://localhost:5173` in your browser
   - Click "Sign In with OAuth" to authenticate
   - Start using the chatbot!

## Tech Stack

### Backend
- Kotlin
- Spring Boot 3.2.0
- Spring Security
- Spring Security OAuth2 Resource Server
- Gradle

### Frontend
- React 18
- TypeScript
- Vite
- Tailwind CSS
- shadcn/ui components
- Lucide icons
- react-oauth2-code-pkce

## Security Features

- **JWT Token Validation**: Backend validates all tokens against OAuth2 provider
- **PKCE Flow**: Frontend uses Proof Key for Code Exchange for enhanced security
- **Stateless Sessions**: No server-side session storage
- **Protected APIs**: All API endpoints require authentication
- **CORS Protection**: Configured to allow only specific origins
- **Secure Token Storage**: Tokens stored securely in browser storage

## Troubleshooting

### Common Issues

1. **"OAuth Client ID not configured"**
   - Ensure you've set `VITE_OAUTH_CLIENT_ID` in frontend `.env` file
   - Restart the dev server after changing `.env` file

2. **"client_secret is missing" error on login**
   - Ensure you've set `VITE_OAUTH_CLIENT_SECRET` in frontend `.env` file
   - Get the Client Secret from your OAuth provider (Google Cloud Console or Auth0)
   - Restart the dev server after changing `.env` file
   - Note: The client secret is required for the OAuth token exchange process

3. **"401 Unauthorized" when calling APIs**
   - Verify your JWT token is valid
   - Check backend logs for token validation errors
   - Ensure OAuth2 issuer URI matches your provider

4. **CORS errors**
   - Add your frontend URL to `cors.allowed-origins` in backend `application.properties`
   - Restart the backend after configuration changes

4. **Redirect URI mismatch**
   - Ensure the redirect URI in `.env` matches the one configured in your OAuth2 provider
   - Check for trailing slashes - they matter!
