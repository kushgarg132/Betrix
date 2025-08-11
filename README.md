# Betrix

Betrix is a full-stack online poker game platform. It features a robust backend for game logic and user management, and a modern frontend for an interactive user experience.

## Features
- Multiplayer online poker games
- Real-time gameplay with WebSocket support
- User authentication and profile management
- Game event logging and replay
- Admin panel for game management
- Responsive UI with card graphics

## Tech Stack
- **Backend:** Java, Spring Boot, Gradle
- **Frontend:** React.js, Axios
- **Database:** (Configure as needed in backend)
- **WebSocket:** Spring WebSocket

## Setup Instructions

### Prerequisites
- Java 17+
- Node.js 16+
- npm or yarn
- (Optional) Docker

### Backend Setup
1. Navigate to the backend directory:
   ```sh
   cd backend
   ```
2. Install dependencies and build:
   ```sh
   ./gradlew build
   ```
3. Run the backend server:
   ```sh
   ./gradlew bootRun
   ```
   The backend will start on the port specified in `application.properties` (default: 8080).

### Frontend Setup
1. Navigate to the frontend directory:
   ```sh
   cd frontend
   ```
2. Install dependencies:
   ```sh
   npm install
   # or
   yarn install
   ```
3. Start the frontend development server:
   ```sh
   npm start
   # or
   yarn start
   ```
   The frontend will start on [http://localhost:3000](http://localhost:3000).

## Running the Project
- Ensure both backend and frontend servers are running.
- Access the app at [http://localhost:3000](http://localhost:3000).
- Register or log in to join a game.

## Folder Structure
```
Betrix/
  backend/    # Java Spring Boot backend
  frontend/   # React frontend
```
- **backend/src/main/java/com/example/backend/**: Main backend source code
- **frontend/src/**: Main frontend source code

## Contribution Guidelines
1. Fork the repository
2. Create a new branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

