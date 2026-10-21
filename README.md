# CollabX 2.0 - Full-Stack Collaborative Coding Workspace

> Migrated from PERN (PostgreSQL, Express, React, Node.js) to **Java 17, Spring Boot 3.3.x, MySQL 8, Spring WebSocket (STOMP), and React**.

---

## 1. Architecture Overview

CollabX 2.0 is structured as a **modular monolith** with a dedicated decoupled React frontend and a robust Java Spring Boot backend:

```
                            +-----------------------------+
                            |       React (Vite)          |
                            |   Monaco Editor + STOMP     |
                            +--------------+--------------+
                                           |
                    REST APIs (HTTP)       |       WebSocket (STOMP / SockJS)
                    [port 8080]            |       [endpoint /ws]
                                           v
+---------------------------------------------------------------------------------+
|                               Spring Boot 3.3.x                                 |
|                                                                                 |
|  +--------------------+  +--------------------+  +---------------------------+  |
|  |  RoomController    |  | CodeExecutionCtrl  |  | CollaborationWsController |  |
|  +---------+----------+  +---------+----------+  +-------------+-------------+  |
|            |                       |                           |                |
|  +---------v----------+  +---------v----------+  +-------------v-------------+  |
|  |    RoomService     |  |   Judge0Service    |  |   CollaborationService    |  |
|  +---------+----------+  +---------+----------+  +---------------------------+  |
|            |                       |                                            |
|  +---------v----------+            | (HTTP Submissions & Polling)               |
|  |   RoomRepository   |            v                                            |
|  +---------+----------+  +--------------------+                                 |
|            |             |     Judge0 API     |                                 |
+------------|-------------+--------------------+---------------------------------+
             v
+-------------------------+
|     MySQL 8 Database    |
| (Flyway Schema Managed) |
+-------------------------+
```

### Key Modules:
1. **`com.collabx.room`**: Manages room creation, cryptographic room ID generation, BCrypt password hashing, and room verification.
2. **`com.collabx.collaboration`**: Real-time STOMP broker endpoints (`/ws`, `/app/room/{roomId}/*`, `/topic/room.{roomId}/*`), thread-safe participant presence tracking, live code synchronization, chat messaging, typing indicators, and shared language state.
3. **`com.collabx.codeexecution`**: Isolated execution via Judge0 API (`POST /api/execute` & `POST /run`). Supports JavaScript, Python, Java, C, C++, TypeScript, SQL, and in-browser HTML Live Preview.
4. **`com.collabx.config`**: Security configuration, CORS policies, WebSocket message broker, and Judge0 properties.

---

## 2. Prerequisites for Windows

Ensure the following tools are installed and available in your `PATH`:
- **Java Development Kit (JDK) 17**: Oracle JDK 17 or Eclipse Temurin 17 (`java -version`)
- **Apache Maven 3.8+**: (`mvn -version`)
- **Node.js 18+ & npm 9+**: (`node -v`, `npm -v`)
- **MySQL Server 8.0+**: Running locally on port `3306`

---

## 3. Quick Start on Windows

### Step 1: Database Setup
Make sure your MySQL service is running. Open PowerShell or MySQL Command Line Client and create the database:

```sql
CREATE DATABASE IF NOT EXISTS collabx_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> **Note**: Flyway automatically executes database migrations upon backend startup (`src/main/resources/db/migration/V1__init_schema.sql`). Tables `rooms` and `chat_messages` are created automatically.

If your MySQL password differs from `root`, set environment variables or edit `backend/src/main/resources/application.yml`:
```powershell
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
```

---

### Step 2: Build and Run Backend (Spring Boot)

Open a PowerShell terminal in the `backend` folder:

```powershell
cd backend
mvn clean spring-boot:run
```

The backend starts at `http://localhost:8080`.

Verify backend health by opening in browser or running:
```powershell
curl http://localhost:8080/health
```
Response:
```json
{
  "service": "CollabX Backend (Spring Boot 3.3.4, Java 17)",
  "status": "ok",
  "timestamp": 1729528000000
}
```

---

### Step 3: Run Frontend (React + Vite)

Open a separate PowerShell terminal in the `frontend` folder:

```powershell
cd frontend
npm install
npm run dev
```

Open your browser at `http://localhost:5173`.

---

## 4. Docker Compose (Alternative Local Setup)

If you prefer running everything in Docker:

```powershell
docker-compose up --build
```

Services started:
- `mysql`: port `3306`
- `backend`: port `8080`
- `frontend`: port `5173`

---

## 5. API Reference

### Room Management

| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/rooms` or `/rooms` | Create a new room with BCrypt password | `{ "roomId"?: string, "username": string, "password": string }` |
| `POST` | `/api/rooms/verify` or `/rooms/verify` | Verify room password before joining | `{ "roomId": string, "password": string }` |
| `GET` | `/api/rooms/{roomId}` or `/rooms/{roomId}` | Fetch room details and saved code | - |
| `GET` | `/health` | Application health status | - |

### Code Execution (Judge0)

| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/execute` or `/run` | Execute code in isolated runner | `{ "roomId": string, "code": string, "language": string, "stdin"?: string, "username"?: string }` |

Supported languages:
- `javascript` (Node.js)
- `python` (Python 3)
- `java` (OpenJDK)
- `c` (GCC)
- `cpp` (GCC C++)
- `typescript` (Node.js/TS)
- `sql` (PostgreSQL / SQLite)
- `html` (Shared Live Browser Preview)

---

## 6. Real-Time STOMP WebSocket Protocol

WebSocket Endpoint: `ws://localhost:8080/ws` (with SockJS fallback `http://localhost:8080/ws`)

### Subscriptions (Receive from Broker):
- `/topic/room.{roomId}.users` -> Online participants list
- `/topic/room.{roomId}.code` -> Code synchronization updates
- `/topic/room.{roomId}.language` -> Shared language state changes
- `/topic/room.{roomId}.chat` -> Team chat messages
- `/topic/room.{roomId}.typing` -> Participant typing indicators
- `/topic/room.{roomId}.execution` -> Execution output and status
- `/topic/room.{roomId}.preview` -> Real-time HTML live preview

### Publications (Send to Backend):
- `/app/room/{roomId}/join` -> Join room & announce presence
- `/app/room/{roomId}/leave` -> Leave room & clean up presence
- `/app/room/{roomId}/code` -> Broadcast code & auto-save to MySQL
- `/app/room/{roomId}/language` -> Change shared editor language
- `/app/room/{roomId}/chat` -> Send chat message
- `/app/room/{roomId}/typing` -> Broadcast typing notification
- `/app/room/{roomId}/preview` -> Update live HTML preview

---

## 7. Testing & Verification

Run all Spring Boot unit & integration tests:

```powershell
cd backend
mvn clean test
```
All 19 tests pass:
- `RoomServiceTest` (room creation, custom ID, BCrypt password hashing, verification, error handling)
- `RoomControllerTest` (REST integration tests for room APIs, HTTP status codes)
- `CollaborationServiceTest` (STOMP presence, session cleanup, broadcasting)
- `Judge0ServiceTest` (language resolution, HTML preview, error handling)

Run frontend build verification:

```powershell
cd frontend
npm run build
```

---

## 8. Environment Variables

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | Backend HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/collabx_db...` | MySQL connection string |
| `SPRING_DATASOURCE_USERNAME` | `root` | MySQL username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | MySQL password |
| `JUDGE0_API_URL` | `https://ce.judge0.com` | Judge0 CE or self-hosted API URL |
| `JUDGE0_API_KEY` | *(empty)* | Optional API key for RapidAPI / authenticated Judge0 |
| `JUDGE0_API_HOST` | *(empty)* | Optional RapidAPI host header |
| `VITE_API_URL` | *(empty)* | Frontend REST API base URL (uses Vite proxy when empty) |
| `VITE_SOCKET_URL` | *(empty)* | Frontend WebSocket URL (uses `/ws` when empty) |
