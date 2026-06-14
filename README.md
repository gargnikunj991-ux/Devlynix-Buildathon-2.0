# Devlynix local integration

This workspace contains the Spring Boot API in `backend-copy` and the Next.js app in `frontend-copy/frontend`.

## Run locally

1. Start PostgreSQL:

   ```powershell
   docker compose up -d
   ```

2. Start the backend in a second terminal:

   ```powershell
   Set-Location backend-copy
   mvn spring-boot:run
   ```

3. Start the frontend in a third terminal:

   ```powershell
   Set-Location frontend-copy/frontend
   Copy-Item .env.example .env.local
   npm install
   npm run dev
   ```

Open `http://localhost:3000`. The frontend calls the API at `http://localhost:8080/api` and stores the returned JWT in browser local storage.

## Configuration

- Frontend API URL: `NEXT_PUBLIC_API_URL`
- Backend database: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- Backend frontend origins: `FRONTEND_ORIGINS` (comma-separated)
- Backend signing key: `JWT_SECRET`

The checked-in defaults are for local development only. Set a new `JWT_SECRET` and production origins before deployment.
