# CollabX 2.0 - Backend Security, Correctness & Integration Hardening

> Production-ready, secure collaborative code editor migrated from PERN to **Java 17, Spring Boot 3.3.x, MySQL 8, Spring WebSocket (STOMP + SockJS), and React 18**.

---

## 1. Architecture & Security Hardening Model

CollabX 2.0 is structured as a **modular monolith** with a decoupled React frontend and a hardened Spring Boot 3.3.x backend.

```
                            +-----------------------------+
                            |       React 18 (Vite)       |
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
|  +---------+----------+  +---------+----------+  +-------------+-------------+  |
|            |                       |                           |                |
|  +---------v----------+            |                           v                |
|  |  RoomTokenService  |            |             +---------------------------+  |
|  |  (HMAC-SHA256)     |            |             | StompAuthChannelIntercept |  |
|  +---------+----------+            |             | (Inbound Subscription Auth|  |
|            |                       |             +---------------------------+  |
|  +---------v----------+            v                           |                |
|  |   RoomRepository   |  +--------------------+                |                |
|  +---------+----------+  |     Judge0 API     |                |                |
|            |             +--------------------+                |                |
+------------|---------------------------------------------------|----------------+
             v                                                   v
+-------------------------+                         +--------------------------+
|     MySQL 8 Database    |                         |  STOMP Simple Broker     |
| (Flyway Schema Managed) |                         |  (/topic/room.{id}.*)    |
+-------------------------+                         +--------------------------+
```

### Key Security & Architectural Enhancements

1. **Cryptographically Secure Room ID Generation**:
   - Room IDs are generated on the backend using `SecureRandom` over an unambiguous URL-safe Base32 alphabet (`23456789ABCDEFGHJKLMNPQRSTUVWXYZ`), delivering over 40 bits of entropy.
   - Uniqueness is strictly enforced at both application logic and database constraint levels (`rooms.room_id` PRIMARY KEY).
   - Duplicate creation attempts with existing IDs are rejected with `400 Bad Request` (`Room ID already in use`) — **rooms can never be overwritten**.
   - Server-generated IDs are returned to the client and authoritative IDs are never derived locally on the frontend.

2. **Room Access Token & Inbound STOMP Authorization**:
   - Room creation and room verification endpoints issue tamper-proof HMAC-SHA256 signed `roomToken` credentials scoped to `(roomId, username, expiresAt)`.
   - `StompAuthChannelInterceptor` intercepts all inbound STOMP frames (`SUBSCRIBE`, `SEND`):
     - **Subscription Isolation**: Clients cannot subscribe to `/topic/room.{roomId}.*` without an authorized token for that room. A participant in Room A cannot read Room B topics.
     - **Anti-Impersonation**: The validated username from session attributes overrides any client-supplied identity in message payloads.
     - **Size Limits**: Payloads over 200KB or chat messages over 1000 characters are rejected at the channel boundary.

3. **Room Password Security & Legacy Migration**:
   - All newly created rooms use `BCryptPasswordEncoder` (strength 10).
   - Plaintext passwords in database records are strictly rejected.
   - For legacy SHA-256 records, verification succeeds and immediately upgrades the stored hash in-place to BCrypt.
   - Passwords and hashes are never logged or exposed in DTO responses.

4. **Strict CORS & WebSocket Origin Policy**:
   - Wildcard origin patterns (`*`) with credentials have been eliminated.
   - Allowed origins are read explicitly from `collabx.cors.allowed-origins` (defaulting to local Vite dev ports `5173` and `3000`).
   - Both REST (`CorsConfig`) and WebSocket endpoints (`WebSocketConfig`) enforce identical origin whitelisting.

5. **Judge0 Isolated Code Execution Hardening**:
   - Secrets (`JUDGE0_API_KEY`) stay on the backend; never exposed to browser.
   - Request bounds enforced via `@Size(max = 65536)` on code and `@Size(max = 10000)` on stdin.
   - Only whitelisted languages are executed (`javascript`, `python`, `java`, `c`, `cpp`, `typescript`, `sql`).
   - Execution broadcasts to `/topic/room.{roomId}.execution` require a valid room token.
   - Maximum execution polling cutoff (30 seconds) prevents thread starvation and unbounded API calls.
   - Browser Live Preview mode (`html`) is explicitly isolated from server execution.

6. **Concurrent Editing Operational Model**:
   - The current synchronization model implements **debounced state snapshot synchronization with anti-echo checks**:
     - Client typing debounces update broadcast by 300ms.
     - Remote updates check `data.username !== currentUsername` before applying to Monaco to prevent infinite echo loops.
     - True concurrent simultaneous multi-cursor character-level editing without last-write-wins collisions would require Operational Transformation (OT) or CRDTs (e.g. Yjs / Automerge). The current model provides lightweight, reliable debounced snapshot synchronization for room collaboration.

---

## 2. Prerequisites for Windows

- **Java Development Kit (JDK) 17**: Oracle JDK 17 or Eclipse Temurin 17 (`java -version`)
- **Apache Maven 3.8+**: (`mvn -version`)
- **Node.js 18+ & npm 9+**: (`node -v`, `npm -v`)
- **MySQL Server 8.0+**: Running locally on port `3306`

---

## 3. Quick Start on Windows

### Step 1: Database Setup
Make sure your MySQL service is running. Open PowerShell or MySQL Workbench:

```sql
CREATE DATABASE IF NOT EXISTS collabx_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> **Flyway**: Migrations run automatically on startup (`src/main/resources/db/migration/V1__init_schema.sql`). Tables `rooms` and `chat_messages` are created automatically with foreign keys and indexes.

If your MySQL credentials differ from `root/root`:
```powershell
$env:SPRING_DATASOURCE_USERNAME="your_user"
$env:SPRING_DATASOURCE_PASSWORD="your_password"
```

---

### Step 2: Build and Run Backend (Spring Boot)

Open a PowerShell terminal in the `backend` directory:

```powershell
cd backend
mvn clean spring-boot:run
```

The backend starts at `http://localhost:8080`.
Verify backend health:
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

Open a separate PowerShell terminal in the `frontend` directory:

```powershell
cd frontend
npm install
npm run dev
```

Open your browser at `http://localhost:5173`.

---

## 4. REST API & STOMP Protocol Reference

### REST Endpoints

| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/rooms` | Create a room (returns server-generated `roomId` & `roomToken`) | `{ "username": string, "password": string, "roomId"?: string }` |
| `POST` | `/api/rooms/verify` | Verify room password (returns `roomToken`) | `{ "roomId": string, "password": string, "username"?: string }` |
| `GET` | `/api/rooms/{roomId}` | Fetch room details and saved code | - |
| `POST` | `/api/execute` | Execute code via Judge0 | `{ "code": string, "language": string, "roomId"?: string, "stdin"?: string, "token"?: string }` |
| `GET` | `/health` | Backend health check | - |

### STOMP Subscriptions & Publications (`ws://localhost:8080/ws`)

| Type | Destination | Authorization |
| :--- | :--- | :--- |
| `SUB` | `/topic/room.{roomId}.users` | Requires valid room token header |
| `SUB` | `/topic/room.{roomId}.code` | Requires valid room token header |
| `SUB` | `/topic/room.{roomId}.language` | Requires valid room token header |
| `SUB` | `/topic/room.{roomId}.chat` | Requires valid room token header |
| `SUB` | `/topic/room.{roomId}.typing` | Requires valid room token header |
| `SUB` | `/topic/room.{roomId}.execution` | Requires valid room token header |
| `PUB` | `/app/room/{roomId}/join` | Validates token, binds session identity |
| `PUB` | `/app/room/{roomId}/code` | Authorized session only (persists to MySQL) |
| `PUB` | `/app/room/{roomId}/chat` | Authorized session only (enforces verified username) |
| `PUB` | `/app/room/{roomId}/language` | Authorized session only |
| `PUB` | `/app/room/{roomId}/leave` | Cleans up session presence |

---

## 5. Automated Testing Verification

All 37 automated backend tests and frontend Vite production builds pass.

### Run Backend Tests:
```powershell
cd backend
mvn clean verify
```

**Results: Tests run: 37, Failures: 0, Errors: 0, Skipped: 0**
- `RoomServiceTest` (10 tests):
  - Server-generated high-entropy room ID verification
  - Duplicate room ID rejection with `BadRequestException`
  - Rejection of invalid room ID characters and short passwords
  - BCrypt password verification and `roomToken` issuance
  - Legacy SHA-256 automatic in-place migration to BCrypt
  - Strict rejection of plaintext passwords
- `RoomControllerTest` (8 tests):
  - `/health` status check
  - `POST /api/rooms` and legacy `/rooms` returns HTTP 201 with `roomToken`
  - Duplicate room ID returns HTTP 400 with "Room ID already in use"
  - Short password returns HTTP 400 validation error
  - Valid password returns HTTP 200; invalid returns HTTP 401; missing returns HTTP 404
- `StompAuthSecurityTest` (7 tests):
  - Unauthorized subscription rejected with `AccessDeniedException`
  - Participant in Room A blocked from subscribing to Room B
  - Participant in Room A blocked from publishing to Room B
  - Valid room token join binds session identity
  - Missing or invalid token join rejected
  - Oversized payloads (>200KB) rejected
- `TwoClientCollaborationIntegrationTest` (1 test):
  - Real-time two-client collaboration between Alice and Bob in Room A
  - Multi-client chat, code sync, and language updates
  - Verification that Eve in Room B receives zero events from Room A
- `CorsConfigTest` (2 tests):
  - Pre-flight requests from configured origin succeed with `Access-Control-Allow-Credentials: true`
  - Pre-flight requests from unconfigured origins receive no `Access-Control-Allow-Origin`
- `Judge0ServiceTest` (3 tests):
  - In-browser HTML live preview execution
  - Unauthenticated/untokened execution does not broadcast to room execution topic
  - Unsupported language throws `BadRequestException`
- `CollaborationServiceTest` (6 tests):
  - Presence tracking, participant deduplication on reconnection, code sync, chat, language state

### Run Frontend Build:
```powershell
cd frontend
npm run build
```
Result: Production bundle compiled successfully with Vite into `dist/`.

---

## 6. Environment Variables Reference

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | Backend port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000` | Whitelisted frontend origins |
| `COLLABX_SECURITY_TOKEN_SECRET` | *(auto-generated)* | 256-bit secret key for signing room access tokens |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/collabx_db...` | MySQL connection string |
| `SPRING_DATASOURCE_USERNAME` | `root` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Database password |
| `JUDGE0_API_URL` | `https://ce.judge0.com` | Judge0 CE or self-hosted API URL |
| `JUDGE0_API_KEY` | *(empty)* | RapidAPI / auth key for Judge0 |
| `JUDGE0_API_HOST` | *(empty)* | RapidAPI host header |
| `JUDGE0_MAX_POLL_ATTEMPTS` | `25` | Maximum polling attempts |
| `JUDGE0_FETCH_TIMEOUT_MS` | `15000` | HTTP client read/connect timeout |
| `VITE_API_URL` | *(empty)* | Frontend REST API base URL |
| `VITE_SOCKET_URL` | *(empty)* | Frontend WebSocket URL |
