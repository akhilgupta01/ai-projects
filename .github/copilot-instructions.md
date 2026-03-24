# ai-projects Repository Conventions

## 1) Repository Structure

- `agents/` : Root directory for all agent implementations.
- `ai-chatbot/`: Root directory for the AI chatbot project, containing separate modules for backend and frontend.
  - `backend/`: Kotlin + Spring Boot API for chat session and security services.
  - `frontend/`: React + TypeScript SPA built with Vite.

Conventions:

- Keep agent-specific logic under `agents/<agent-name>/`.

## 2) Technology Standards

- Languages
  - Kotlin (JVM services)
  - TypeScript (frontend)
- Backend frameworks
  - Spring Boot
  - Spring Security / OAuth2 Resource Server (chatbot backend)
- AI/LLM integration
  - koog agent framework
  - Google GenAI SDK
  - Vertex AI Gemini
- Frontend stack
  - React 18
  - Vite
  - Tailwind CSS + Radix UI/shadcn component patterns

Conventions:

- Target Java 17 for Kotlin services.
- Use Kotlin DSL Gradle files (`build.gradle.kts`) for JVM modules.
- Keep frontend in ESM mode and TypeScript strictness as configured by existing tsconfig files.

## 3) Build Tooling and Commands

- All modules must be buildable using Gradle.
- JVM modules use Gradle Wrapper only (`./gradlew`).
- Frontend uses npm scripts from `package.json`.

Conventions:

- Use the repository-level Gradle wrapper as the primary build entry point.
- Do not use globally installed Gradle binaries in CI or docs; always use wrapper scripts.
- Keep scripts reproducible and non-interactive.

## 4) Dependency Version Management

Conventions:

- Use shared Gradle version catalog (`gradle/libs.versions.toml`) for multi-module JVM dependency consistency
