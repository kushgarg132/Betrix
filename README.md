# ♠️ Betrix Poker

Betrix is a full-stack Texas Hold'em poker platform with AI-powered bots and real-time GraphQL
updates. Chips are play money — every player sits down with the same fixed buy-in; there is no
real-money path.

![Betrix Logo](frontend/public/favicon.png)

## 🌐 Live Demo
- **Platform:** [betrix-b3c24.web.app](https://betrix-b3c24.web.app/)
- **Backend:** `https://betrix.161.118.167.148.nip.io` (Docker Compose on a VM; GraphQL at `/graphql`)

API docs (`/swagger-ui`, `/graphiql`) and schema introspection are disabled by default in
production — see `SWAGGER_ENABLED` / `GRAPHIQL_ENABLED` / `GRAPHQL_INTROSPECTION_ENABLED` below.

## ✨ Features
- **🤖 AI Opponents:** Bots powered by Google Gemini, with a local random fallback if the API is
  unavailable, disabled, or past its per-minute call budget.
- **📡 Real-time Gameplay:** GraphQL subscriptions over WebSocket.
- **🛡️ Auth:** JWT-based, with guest logins (guest-/bot- usernames are reserved and cannot be
  registered).
- **🔄 Game Replay:** Event log + replay, admin-only (`gameEvents`, `replayGame`).
- **📱 Responsive Design:** Built with Tailwind CSS 4, Framer Motion, and Radix UI.

The API is GraphQL only — there are no REST endpoints beyond the framework's own actuator health
check and, when enabled, Swagger's own UI.

## 🛠️ Tech Stack

### Backend
- **Core:** Java 21, Spring Boot 3.5
- **API Layer:** GraphQL (Spring for GraphQL) over HTTP and WebSocket
- **Database:** MongoDB (Atlas in production; no local DB container)
- **AI Integration:** Google Gemini (model ids configurable, see `.env.example`)
- **Security:** Spring Security, JWT (jjwt)
- **Build Tool:** Gradle

### Frontend
- **Framework:** React 18 (Vite)
- **State Management:** Apollo Client (GraphQL)
- **Styling:** Tailwind CSS 4
- **Animations:** Framer Motion
- **Components:** Radix UI, Lucide Icons
- **Deployment:** Firebase Hosting

## 🚀 Setup Instructions

### Prerequisites
- **Java 21+**
- **Node.js 18+**
- **MongoDB** (Atlas connection string, or a local instance)
- **Gemini API Key** (optional — bots fall back to random play without one)

### Backend Setup
1. From the **repo root** (docker-compose.yml and the shared `.env` live here, not in `backend/`),
   copy `.env.example` to `.env` and fill it in. `APP_JWT_SECRET` and `ADMIN_PASSWORD` must meet
   the minimums noted in `.env.example` or the app refuses to start.
2. Run with Docker Compose (this is what actually ships):
   ```sh
   docker compose up -d --build backend
   ```
   Or, for local iteration without Docker, from `backend/`:
   ```sh
   cd backend
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```
   The `local` profile turns GraphiQL, introspection and Swagger UI on.

### Frontend Setup
1. Navigate to the frontend directory:
   ```sh
   cd frontend
   ```
2. Install dependencies:
   ```sh
   npm install
   ```
3. Start the development server:
   ```sh
   npm start
   ```
   The frontend will start on [http://localhost:3000](http://localhost:3000).

## 📂 Project Structure
```text
Betrix/
├── backend/            # Spring Boot Application
│   ├── src/main/java   # Java Source Code
│   └── src/main/resources # Config & GraphQL Schemas
├── frontend/           # Vite + React Application
│   ├── src/            # Components, Hooks, & Logic
│   └── public/         # Static Assets
├── docker-compose.yml  # Backend + how it's actually deployed
└── .env.example        # Every env var the backend reads, with defaults noted
```

## 🤝 Contributing
1. Fork the project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
