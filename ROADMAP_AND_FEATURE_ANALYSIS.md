# Devlynix — Roadmap, Architecture & Feature Analysis

**Date:** October 2026  
**Document Version:** 2.0  
**Backend:** Spring Boot 3.3.5 / Java 21 / Neon PostgreSQL / Render  
**Frontend:** Next.js 16 (Turbopack) / React 19 / Tailwind CSS v4 / Vercel  

---

## 1. Executive Summary & Live Infrastructure State

Devlynix is a developer pairing platform inspired by Tinder, tailored specifically for hackathon builders, engineers, and open-source contributors. Developers create a profile showcasing technical skills, discover potential collaborators, swipe to connect, and communicate via real-time messaging.

### Live Infrastructure State

| Component | Provider / Stack | Deployment Status | Live URL |
| :--- | :--- | :--- | :--- |
| **Backend API** | Render (Web Service, Docker/Maven, Java 21) | **LIVE** (`dep-dav8e9942hec738386pg`) | `https://devlynix-buildathon-2-0.onrender.com` |
| **Database** | Neon Cloud (PostgreSQL 16, Pooled connection) | **CONNECTED** (via HikariCP & `DataSourceConfig`) | Neon Serverless Host |
| **Frontend UI** | Vercel (Next.js 16, React 19, TypeScript) | **LIVE** (`main` branch) | `https://devlynix-frontend12-git-main-hxmblevishus-projects.vercel.app` |
| **Health Check** | Spring Boot Actuator / Custom Health Endpoint | **HEALTHY** (`{"status":"ok"}`) | `/api/health` |
| **Continuous Delivery**| GitHub Actions (`.github/workflows/ci-cd.yml`) | **CONFIGURED** | Auto-triggered on `push` |

---

## 2. Feature Implementation Status: Backend vs. Frontend Matrix

All major foundational capabilities planned in v1.0 and v1.1 have been **fully implemented end-to-end**:

| Feature Area | Backend API | Frontend Client (`lib/api.ts`) | Frontend UI (`app/`) | Status |
| :--- | :--- | :--- | :--- | :---: |
| **Incoming Match Requests (Signal Radar)** | `GET /api/matches/requests` | `api.getIncomingRequests` | Signal Radar corner widget + `IncomingRequestsModal` with Accept/Decline actions | 🟢 **Complete** |
| **Skill-Based Discovery Filtering** | `GET /api/discover?skill={skill}` | `api.discover(token, skill, page, size)` | Retro tag selector bar with preset chips, custom search, and reset filter button | 🟢 **Complete** |
| **Profile Editing / Updating** | `PUT` & `PATCH /api/profile/me` | `api.updateProfile` | `EditProfileModal` with bio, location, goals, and interactive skill chip manager | 🟢 **Complete** |
| **GitHub Links & Avatar Resolution** | `githubUrl` field in User entity | `getAvatarUrl` & `getGithubUsername` | Clickable `[GITHUB]` links + auto-resolved GitHub avatar images | 🟢 **Complete** |
| **Discovery Queue Rewind (Reset Passes)** | `DELETE /api/discover/reset-passes` | `api.resetPasses` | Rewind Queue button on empty candidate feed to re-review skipped profiles | 🟢 **Complete** |
| **Unmatching & Conversation Severance** | `DELETE /api/matches/{matchId}` | `api.unmatch` | Unmatch action inside `TeammateIntelPanel` chat drawer | 🟢 **Complete** |
| **Chat Message History & Clear Chat** | `GET` & `DELETE /api/chat/{matchId}/messages` | `api.getMessages`, `api.clearChat` | Real-time chat feed with Clear Chat option in `TeammateIntelPanel` | 🟢 **Complete** |
| **Unread Indicators & Read Receipts** | `PUT /api/chat/{matchId}/read` | `api.markChatAsRead` | Automatic read marking on active match conversation | 🟢 **Complete** |
| **Adaptive Delta Polling Sync** | `GET /api/chat/{matchId}/messages?after={id}` | Incremental delta fetch | 2s active poll / 30s background backoff via Page Visibility API | 🟢 **Complete** |
| **Optimistic Chat UI & Audio FX** | Supported via REST persistence | Optimistic state update | Instant message rendering + Web Audio API synthesizer clicks & chimes | 🟢 **Complete** |
| **Refresh Token Rotation (RTR)** | `POST /api/auth/refresh` | `api.refreshToken` + 401 interceptor | Silent automatic token refresh with multi-request concurrency mutex lock | 🟢 **Complete** |
| **Multi-Device Session Telemetry** | `GET /api/auth/sessions`, `DELETE /sessions/{id}`, `POST /terminate-others` | `api.getSessions`, `api.terminateSession`, etc. | Dedicated Cyberpunk `/sessions` page with device info, IP, and remote kill switches | 🟢 **Complete** |
| **Sliding-Window Rate Limiting** | `RateLimitFilter.java` (60 req/min) | Handles HTTP 429 status | Protects all API endpoints against abuse with standard RFC headers | 🟢 **Complete** |
| **Automated Database Cleanup** | Hourly `@Scheduled` Spring worker | Automatic server-side | Purges expired refresh tokens and aged revoked records (> 24h) | 🟢 **Complete** |

---

## 3. Architecture Highlights

### 3.1 Security & Token Family Architecture
1. **Short-Lived Access Tokens:** Expire in 10–15 minutes, mitigating the impact of token interception.
2. **Rotating Refresh Tokens:** Valid for 7 days, renewed on every `/api/auth/refresh` invocation.
3. **Compromise / Replay Attack Defense:**
   - Every login session generates a distinct `familyId`.
   - When rotated, the prior token is tagged as `revoked = true` with a timestamp and retained for 24 hours.
   - If an attacker attempts to reuse an already-revoked refresh token, the server detects the replay attempt and immediately invalidates the **entire `familyId`**, revoking access across that device session.
4. **Selective Multi-Device Logout:**
   - Logging out of a mobile browser deletes only that device's session token family; desktop and laptop sessions remain active.
   - `/sessions` page enables remote termination of lost or compromised devices.

### 3.2 Adaptive Real-Time Messaging Engine
- **Why Adaptive Polling over Pure WebSockets:**
  - Standard WebSockets on serverless or sleepable instances (Render free-tier, Neon scale-to-zero) suffer from broken socket connections and high idle resource overhead.
  - Devlynix uses an **Adaptive HTTP Delta Polling** system:
    - Queries only new messages via `?after={latestMessageId}`.
    - Polls every 2 seconds while active.
    - Uses the browser's `visibilitychange` API to automatically back off to 30 seconds when the tab is backgrounded.
    - Refocusing the tab triggers an immediate poll.
  - Result: Bandwidth and database operations reduced by over 90% without sacrificing real-time feel.

---

## 4. Strategic Roadmap: Future Enhancements

### Phase 1: Foundational Enhancements (100% Completed ✅)
- [x] Incoming Match Requests (Signal Radar widget & modal)
- [x] Tech stack filter bar with preset chips & arbitrary stack search
- [x] Developer Profile editing modal (`EditProfileModal`)
- [x] Clickable GitHub links & avatar resolution
- [x] Queue rewind / reset skipped passes (`/reset-passes`)
- [x] Unmatch & conversation severance (`/matches/{id}`)
- [x] Clear conversation history (`DELETE /messages`)
- [x] Refresh Token Rotation (RTR) with token family compromise defense
- [x] Multi-device active session controls (`/sessions` page)
- [x] Adaptive chat delta polling with Page Visibility API backoff
- [x] Web Audio procedural audio feedback (swipes, matches, messages)

### Phase 2: Production Hardening & Operations (Current Focus)
- [ ] **Render Keep-Alive Cron:** Scheduled ping every 12 minutes to prevent Render free-tier cold starts.
- [ ] **Interactive API Documentation:** Integrate `springdoc-openapi-starter-webmvc-ui` for live `/swagger-ui.html`.
- [ ] **Database Migration Tool:** Transition Hibernate `ddl-auto=update` to **Flyway** migrations.
- [ ] **Input Sanitization (XSS Prevention):** Add OWASP HTML sanitizer on bios and project pitches.

### Phase 3: Advanced Collaboration Features
- [ ] **Hackathon Project Pitches:** Allow developers to showcase their specific project concept with mockups.
- [ ] **Squad / Team Formation:** Support multi-user squad formation (teams of 3–4 members) alongside 1-on-1 pairs.
- [ ] **AI-Powered Synergy Scoring:** Machine learning matching that pairs complementary disciplines (e.g., Frontend engineers with Backend/ML engineers).
- [ ] **Distributed Rate Limiting:** Migrate in-memory token bucket to **Redis** for multi-instance deployments.

---

## 5. Architectural File Map

### Backend (`Devlynix-Buildathon-2.0`)
- `com.devtinder.controller.AuthController` — Auth, RTR, and session management endpoints.
- `com.devtinder.controller.ProfileController` — Profile retrieval and updates.
- `com.devtinder.controller.DiscoverController` — Discover feed, swiping, and pass reset.
- `com.devtinder.controller.MatchController` — Matches list, incoming requests radar, unmatch.
- `com.devtinder.controller.ChatController` — Message history, delta sync, read marking, chat clear.
- `com.devtinder.security.SecurityConfig` — Stateless Spring Security chain and CORS configuration.
- `com.devtinder.security.RateLimitFilter` — Sliding-window rate limiter (60 req/min).
- `com.devtinder.service.RefreshTokenService` — Token family rotation, theft detection, hourly purge.
- `com.devtinder.websocket.WebSocketConfig` — STOMP message broker configuration.

### Frontend (`devlynix-frontend`)
- `lib/api.ts` — 20 typed API methods, silent token refresh interceptor, GitHub avatar utilities.
- `lib/session.ts` — Local storage session sync and multi-tab coordination.
- `lib/sound.ts` — Zero-dependency Web Audio synthesizers for retro UI audio feedback.
- `app/dashboard/page.tsx` — Candidate card discovery, signal radar, tech stack filter.
- `app/matches/page.tsx` — Matches list, adaptive delta polling chat, teammate intel panel.
- `app/sessions/page.tsx` — Active device sessions manager with telemetry and remote termination.
- `components/theme/` — 16 custom cyberpunk retro components.
