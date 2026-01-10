# AI Operational Support ChatBot

An AI-assisted operational support chatbot application with a React/TypeScript frontend and Kotlin/SpringBoot backend.

## Project Structure

```
ai-chatbot/
├── backend/          # Kotlin/SpringBoot backend
└── frontend/         # React/TypeScript frontend
```

## Backend (Kotlin/SpringBoot)

### Features
- REST APIs for chat session management
- Create, list, get, and delete chat sessions
- Send messages and receive AI assistant responses
- In-memory storage for chat sessions
- CORS configured for frontend communication

### API Endpoints
- `POST /api/sessions` - Create a new chat session
- `GET /api/sessions` - Get all chat sessions
- `GET /api/sessions/{sessionId}` - Get a specific session with messages
- `POST /api/sessions/{sessionId}/messages` - Send a message and get bot response
- `DELETE /api/sessions/{sessionId}` - Delete a chat session

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

## Configuration

### Frontend Environment Variables

Create a `.env` file in the frontend directory:

```
VITE_API_URL=http://localhost:8080
```

Update `VITE_API_URL` to point to your deployed backend URL in production.

## Tech Stack

### Backend
- Kotlin
- Spring Boot 3.2.0
- Gradle

### Frontend
- React 18
- TypeScript
- Vite
- Tailwind CSS
- shadcn/ui components
- Lucide icons
