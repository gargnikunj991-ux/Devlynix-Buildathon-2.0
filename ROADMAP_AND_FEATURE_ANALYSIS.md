# Devlynix — Roadmap, Architecture & Feature Gap Analysis

**Date:** October 2026  
**Document Version:** 1.1  
**Backend:** Spring Boot 3.3.5 / Java 21 / Neon PostgreSQL / Render  
**Frontend:** Next.js 16 (Turbopack) / React 19 / Tailwind CSS / Vercel  

---

## 1. Executive Summary & Current Status

Devlynix is a developer pairing platform inspired by Tinder, tailored specifically for hackathon builders, engineers, and open-source contributors. Developers create a profile showcasing technical skills, discover potential collaborators, swipe to connect, and communicate via real-time messaging.

### Live Infrastructure State

| Component | Provider / Stack | Deployment Status | Live URL |
| :--- | :--- | :--- | :--- |
| **Backend API** | Render (Web Service, Docker/Maven, Java 21) | **LIVE** (`dep-dav8e9942hec738386pg`) | `https://devlynix-buildathon-2-0.onrender.com` |
| **Database** | Neon Cloud (PostgreSQL 16, Pooled connection) | **CONNECTED** (via HikariCP & `DataSourceConfig`) | Neon Serverless Host |
| **Frontend UI** | Vercel (Next.js 16, React 19, TypeScript) | **LIVE** (`main` branch) | `https://devlynix-frontend12-git-main-hxmblevishus-projects.vercel.app` |
| **Health Check** | Spring Boot Actuator / Custom Health Endpoint | **HEALTHY** (`{"status":"ok"}`) | `/api/health` |
| **Continuous Delivery**| GitHub Actions (`.github/workflows/ci-cd.yml`) | **CONFIGURED** | Auto-triggered on `push` |

### Verified End-to-End Capabilities
1. **User Authentication**: Registration with hashed passwords (BCrypt) and JWT token generation.
2. **Developer Discovery**: Querying available developers excluding already-swiped users.
3. **Mutual Matching**: Instant bidirectional match generation when two developers swipe `LIKE` on each other.
4. **Chat Messaging**: Message persistence in PostgreSQL and real-time polling synchronization (2-second interval with smooth scrolling).

---

## 2. Feature Gap Analysis: Backend Capabilities vs. Frontend UI

A comprehensive audit of the backend codebase reveals several implemented backend APIs, database columns, and features that are either **not exposed** or **partially exposed** in the current frontend user interface.

| Feature Area | Backend Implementation | Frontend Client (`lib/api.ts`) | Frontend UI (`app/`) | Status / Gap |
| :--- | :--- | :--- | :--- | :--- |
| **Incoming Match Requests ("Who Liked You")** | Swipes are recorded in database (`Swipe` entity), but no dedicated query exists yet | Not implemented | Not implemented | 🔴 **Missing End-to-End**: Users cannot see who has sent them a match request; requires new backend endpoint + corner/drawer UI. |
| **Skill-Based Discovery Filtering** | `GET /api/discover?skill={skill}` (`DiscoverController.java:31`) | `api.discover(token, skill)` supported | `app/dashboard/page.tsx` calls `api.discover(token)` with no parameter. | 🔴 **Missing UI**: No skill filter bar, tag selector, or dropdown to filter candidates by skill. |
| **Profile Editing / Updating** | `PUT /api/profile/me` with `UpdateProfileRequest` (`name`, `bio`, `githubUrl`, `lookingFor`, `location`, `skills`) | `api.updateProfile(token, payload)` defined | Not referenced anywhere in `app/` or `components/`. | 🔴 **Missing UI**: Users cannot edit their profile, add/remove skills, or update bios after registration. |
| **GitHub Profile Links** | `githubUrl` field in `User` entity, `RegisterRequest`, and `ProfileResponse` | Returned in `Profile` interface | Collected on `/register`, but never rendered as a clickable link on cards or matches. | 🟡 **Missing Link**: No "View GitHub Profile" button/link on developer discovery cards or chat header. |
| **Location & Availability Display** | `location` & `lookingFor` fields stored and returned in `ProfileResponse` | Defined in `Profile` interface | Only displays `bio \|\| lookingFor` as single string fallback. `location` is omitted. | 🟡 **Partial UI**: Teammate's location/timezone and target project intent (`lookingFor`) are not cleanly broken out. |
| **STOMP WebSocket Messaging** | `/ws` SockJS endpoint + `/topic/matches/{matchId}` + `@MessageMapping("/chat.send")` | Not implemented in `lib/api.ts` | Uses 2-second HTTP polling interval (`app/matches/page.tsx`) | 🟢 **Functional Alternative**: Polling works reliably; full WebSocket client (`@stomp/stompjs`) can be integrated for sub-100ms delivery. |
| **Rate Limit Feedback** | `RateLimitFilter.java` returning `429 Too Many Requests` with `X-RateLimit-*` headers | Standard fetch handler throws generic `ApiError` | Generic error toast/alert | 🟡 **Missing UI**: No countdown or specific notification explaining request throttling. |
| **Teammate Profile in Chat** | `MatchResponse` includes full `ProfileResponse` (`skills`, `bio`, `githubUrl`, `createdAt`) | Stored in `Match.user` | `app/matches/page.tsx` header only shows `user.name`. | 🟡 **Underutilized Data**: Chat screen does not show teammate's skills, bio, or contact info. |
| **Unmatching / Conversation Archival** | Not implemented in backend | Not implemented | Not implemented | ⚪ **Future Feature**: Neither backend nor frontend supports unmatching or deleting conversation history. |

---

## 3. Detailed Breakdown & Architecture of Key Features

### 3.1 Incoming Match Requests Section ("Who Liked You" / Pending Invites)
Currently, matching is a blind double-opt-in: Developer A likes Developer B, but Developer B has no idea Developer A is interested until Developer B happens to stumble upon Developer A in the general discovery feed and swipes right.

#### Proposed UI Experience:
- **Corner Notification / Drawer**: In the top-right corner of the Dashboard (or a persistent floating radar widget in the bottom corner), display an **`INCOMING SIGNALS [N]`** counter badge.
- **Card Preview**: Clicking it expands a drawer or modal displaying developers who have already liked your profile.
- **Immediate Match Action**: Each card includes:
  - Developer's Name, GitHub link, Skills, and Hackathon Goals.
  - **`[ACCEPT & MATCH]`** button: Calling the existing `POST /api/discover/swipe` (`direction: 'LIKE'`), instantly producing a match and opening the chat channel!
  - **`[DECLINE]`** button: Calling `POST /api/discover/swipe` (`direction: 'PASS'`), quietly archiving the request.

#### Required Backend Additions:
1. **Repository Query (`SwipeRepository.java`)**:
   ```java
   @Query("""
       select s.swiper
       from Swipe s
       where s.swiped.id = :userId
         and s.direction = 'LIKE'
         and s.swiper.id not in (
             select s2.swiped.id
             from Swipe s2
             where s2.swiper.id = :userId
         )
       order by s.createdAt desc
   """)
   List<User> findIncomingPendingLikes(@Param("userId") Long userId);
   ```
2. **Controller Endpoint (`DiscoverController.java` or `MatchController.java`)**:
   ```java
   @GetMapping("/requests")
   public List<ProfileResponse> getIncomingRequests(@AuthenticationPrincipal UserDetails userDetails) {
       return matchService.getIncomingRequests(userDetails.getUsername());
   }
   ```
3. **Frontend API Call (`lib/api.ts`)**:
   ```typescript
   getIncomingRequests(token: string) {
       return request<Profile[]>("/matches/requests", {}, token);
   }
   ```

---

### 3.2 Skill-Based Filtering in Discovery
- **Backend Capability**: `DiscoverService.java` filters candidates using:
  ```java
  filter(candidate -> required == null || lowerSkillSet(candidate).contains(required))
  ```
  It already accepts any skill query parameter (`GET /api/discover?skill=React`).
- **Frontend Opportunity**:
  - Add a retro skill filter bar above the discovery card grid in `app/dashboard/page.tsx`.
  - Let users click on their own skills or type in a search box to see only developers proficient in that stack (e.g., "Show me Rust devs", "Show me Next.js devs").

---

### 3.3 Profile Editing (Settings / Edit Modal)
- **Backend Capability**: `PUT /api/profile/me` accepts partial or full updates for:
  - `name`: string (max 120)
  - `githubUrl`: string (max 260)
  - `bio`: string (max 600)
  - `lookingFor`: string (max 160)
  - `location`: string (max 120)
  - `skills`: string list
- **Frontend Opportunity**:
  - Add an `[EDIT PROFILE]` button next to "AUTHENTICATED DEVELOPER" in `app/dashboard/page.tsx`.
  - Render an interactive `RetroModal` with fields to adjust bio, update hackathon goals, and add new skills as developers learn them.

---

### 3.4 GitHub & Social Integration
- **Backend Capability**: `githubUrl` is stored in the `users` table and returned with every profile payload.
- **Frontend Opportunity**:
  - Add an external link icon / button (`[GITHUB]`) on each card in `app/dashboard/page.tsx` and in the chat header in `app/matches/page.tsx`.
  - Auto-fetch the developer's GitHub avatar using `https://github.com/${username}.png` as an optional enhancement.

---

### 3.5 Match Profile Drawer in Chat
- **Backend Capability**: Full teammate metadata is provided in `MatchResponse.user`.
- **Frontend Opportunity**:
  - In `app/matches/page.tsx`, add a collapsible sidebar or header popover showing:
    - Teammate skills tags
    - Looking For / Hackathon goals
    - Timezone / Location
    - Direct GitHub link

---

### 3.6 Additional Backend Capabilities & System Extensions

1. **Instant Match Alert Push (WebSocket)**:
   - When User B swipes `LIKE` on User A, completing the match, backend can publish an instant notification to `/topic/user/{userAId}/notifications`:
     ```json
     { "type": "NEW_MATCH", "matchId": 2, "user": { "name": "Alex Rivers", ... } }
     ```
   - User A sees an instant cyberpunk celebratory modal: *"MATCH FOUND! YOU AND ALEX CAN NOW CHAT"*.

2. **Unread Message Indicators (`isRead` / `readAt`)**:
   - Currently, `Message` entity has `id`, `match`, `sender`, `content`, `sentAt`.
   - Adding `isRead boolean default false` or `readAt Instant` enables unread message counter badges on the matches sidebar and push notifications.

3. **Discovery Reset / Rewind ("Reset Passes")**:
   - Currently, `UserRepository.findDiscoverableUsers` permanently excludes anyone the user has swiped on (whether `LIKE` or `PASS`).
   - In small developer communities or fast-paced hackathons, a user may run out of profiles quickly.
   - Adding `DELETE /api/discover/reset-passes` allows developers to wipe their negative passes (`direction = 'PASS'`) and re-evaluate developers they previously skipped.

4. **Online Presence & Last Seen**:
   - Hooking into Spring WebSocket `SessionConnectedEvent` and `SessionDisconnectEvent` allows tracking online status in-memory or Redis.
   - Shows green status lights on online teammates in the matches sidebar.

---

## 4. Strategic Roadmap & Next Steps

### Phase 1: High-Impact UI Completion (Immediate Priority)
- [ ] **Incoming Match Requests Corner Widget**: Add an `INCOMING SIGNALS` badge and drawer showing developers who liked your profile, with instant Accept/Decline actions.
- [ ] **Skill Filter Bar**: Add a chip-based skill selector on the Dashboard to filter discovery cards by technology.
- [ ] **Edit Profile Modal**: Build a retro-styled modal on the Dashboard calling `api.updateProfile` to edit user info and skills.
- [ ] **GitHub Links**: Display clickable GitHub badges on both discovery cards and active chat headers.
- [ ] **Location & Goal Badges**: Display distinct badges for `location` (e.g., "Remote", "Bangalore") and `lookingFor` (e.g., "Seeking Backend Lead").

### Phase 2: Reliability & Production Hardening
- [ ] **Render Keep-Alive Cron**:
  - Render free-tier web services sleep after 15 minutes of inactivity.
  - Set up a scheduled ping every 10–14 minutes calling `GET https://devlynix-buildathon-2-0.onrender.com/api/health`.
  - Can be configured via a free cron service (e.g., [Cron-job.org](https://cron-job.org) or [UptimeRobot](https://uptimerobot.com)) or a GitHub Actions scheduled workflow (`schedule: - cron: '*/12 * * * *'`).
- [ ] **GitHub Actions Webhook Deployment**:
  - Add `RENDER_DEPLOY_HOOK_URL` to GitHub repository secrets (`Settings -> Secrets and variables -> Actions`) so `.github/workflows/ci-cd.yml` automatically triggers an instant deploy upon merging to `main`.
- [ ] **Database Connection Resiliency**:
  - Current Hikari configuration handles Neon scale-to-zero cold boots via `initializationFailTimeout = -1`. Ensure Hikari maximum lifetime and idle timeouts remain aligned with Neon connection drop limits.

### Phase 3: Communication & Collaboration Upgrades
- [ ] **Full WebSocket Client Integration**:
  - Replace or augment the 2-second HTTP polling with a STOMP over SockJS client connection using `@stomp/stompjs`.
  - Provides instant sub-second delivery for active chat rooms.
- [ ] **Typing Indicators & Read Receipts**:
  - Broadcast ephemeral typing events over `/topic/matches/{matchId}/typing`.
- [ ] **Unmatch & Block**:
  - Add backend `DELETE /api/matches/{matchId}` endpoint to allow users to disconnect cleanly.

### Phase 4: Hackathon & Team Formation Features
- [ ] **Project Showcases**:
  - Allow users to attach a "Project Pitch" or "Hackathon Idea" to their profile.
- [ ] **Multi-Person Team Matching**:
  - Enable squads of 3–4 developers rather than solely 1-on-1 pairs.
- [ ] **AI-Powered Complementary Match Scoring**:
  - Suggest pairs based on complementary skills (e.g., match Frontend engineers with Backend/ML engineers rather than identical skillsets).

---

## 5. Architectural Reference & File Map

### Backend (`D:\Devlynix-Buildathon-2.0`)
- `com.devtinder.config.DataSourceConfig`: JDBC parser for Postgres/Neon pooled URIs.
- `com.devtinder.controller.AuthController`: `/api/auth/register`, `/api/auth/login`.
- `com.devtinder.controller.ProfileController`: `/api/profile/me` (`GET`, `PUT`).
- `com.devtinder.controller.DiscoverController`: `/api/discover` (`GET` with `?skill=`), `/api/discover/swipe` (`POST`).
- `com.devtinder.controller.MatchController`: `/api/matches` (`GET`).
- `com.devtinder.controller.ChatController`: `/api/chat/{matchId}/messages` (`GET`, `POST`), broadcasts to `/topic/matches/{matchId}`.
- `com.devtinder.websocket.WebSocketConfig`: STOMP message broker configuration (`/ws`, `/topic`, `/app`).
- `com.devtinder.security.SecurityConfig`: Stateless JWT security, BCrypt encoder, CORS configuration.
- `com.devtinder.security.RateLimitFilter`: Token-bucket 60 req/min rate limiter.

### Frontend (`D:\devlynix-frontend`)
- `lib/api.ts`: Centralized fetch client with typed request methods.
- `lib/session.ts`: Local storage session management and token persistence.
- `app/dashboard/page.tsx`: Developer discovery card deck with PASS / LIKE actions.
- `app/matches/page.tsx`: Match list with live 2-second message polling and auto-scroll.
- `app/login/page.tsx` & `app/register/page.tsx`: Authentication flows with retro styling.
- `components/theme/`: Custom cyberpunk cassette retro UI components.
