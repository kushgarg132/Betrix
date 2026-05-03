# ♠️ Betrix Poker

Betrix is a premium, full-stack Texas Hold'em poker platform designed for a high-stakes, immersive gaming experience. Featuring AI-powered bots, real-time GraphQL updates, and a stunning modern UI, Betrix brings the casino experience directly to your browser.

![Betrix Logo](frontend/public/favicon.png)

## 🌐 Live Demo
- **Platform:** [betrix-b3c24.web.app](https://betrix-b3c24.web.app/)
- **API Docs:** [Swagger UI](https://betrix-backend.onrender.com/swagger-ui/index.html)

## ✨ Features
- **🤖 AI Opponents:** Play against intelligent bots powered by **Google Gemini AI**.
- **📡 Real-time Gameplay:** Seamless updates using **GraphQL Subscriptions** and WebSockets.
- **💎 Premium UI/UX:** A high-end interface built with **Tailwind CSS 4**, **Framer Motion**, and **Radix UI**.
- **🛡️ Secure Auth:** Robust user authentication system with support for guest logins.
- **📊 API Documentation:** Fully documented REST endpoints via **Swagger/OpenAPI**.
- **🔄 Game Replay:** Analyze your gameplay with event logging and replay capabilities.
- **📱 Responsive Design:** Optimized for all devices, from desktops to mobile phones.

## 🛠️ Tech Stack

### Backend
- **Core:** Java 21, Spring Boot 3.4
- **API Layer:** GraphQL, REST
- **Database:** MongoDB
- **AI Integration:** Google Gemini AI (Flash 1.5 & Pro)
- **Security:** Spring Security, JWT
- **Documentation:** SpringDoc OpenAPI (Swagger)
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
- **MongoDB** (Local or Atlas)
- **Gemini API Key** (Optional, for AI bots)

### Backend Setup
1. Navigate to the backend directory:
   ```sh
   cd backend
   ```
2. Create a `.env` file based on the environment configuration:
   ```env
   SERVER_PORT=8080
   APP_JWT_SECRET=your_secret_key
   SPRING_DATA_MONGODB_URI=your_mongodb_uri
   GEMINI_API_KEY=your_gemini_key
   GEMINI_ENABLED=true
   ```
3. Build and run:
   ```sh
   ./gradlew bootRun
   ```

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
   npm run dev
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
└── README.md           # Documentation
```

## 🤝 Contributing
1. Fork the project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---
Built with ❤️ by the Betrix Team.
