# EduPulse — AI-Based Student Academic Performance Prediction & Early Support System

Spring Boot 3 + MongoDB backend, React + Tailwind frontend. Single in-process
rule-based scoring baseline (no Python, no scikit-learn — kept small per the
"simple application" requirement). Everything AI goes through one
`AiService` class that supports a **demo mode** (deterministic mocks) whenever
`GEMINI_API_KEY` is blank — paste a real key in `.env` to flip to live Gemini.

## Project layout

```
backend/      Spring Boot 3 (Java 21) + MongoDB + Gemini gateway
frontend/     Vite + React + Tailwind (monochrome UI, no gradients)
docker-compose.yml
```

## Run locally

```bash
# 1) start Mongo
docker run -d -p 27017:27017 mongo:7
# or: brew services start mongodb-community

# 2) backend
cd backend
cp .env.example .env
./mvnw spring-boot:run    # http://localhost:5050
# To seed the demo data on first boot:
EDUPULSE_SEED=true ./mvnw spring-boot:run
# Or pass --seed to the run command line.

# 3) frontend
cd ../frontend
npm install
npm run dev                # http://localhost:5173
```

One command, all services:

```bash
docker compose up --build
```

## Demo credentials (after seed)

| Role     | Email                       | Password    |
|----------|-----------------------------|-------------|
| Teacher  | `teacher@edupulse.dev`      | `password123` |
| Admin    | `admin@edupulse.dev`        | `password123` |
| Student  | `student1@edupulse.dev`     | `password123` |

## Environment variables

| Var | Default | Purpose |
|-----|---------|---------|
| `MONGO_URI` | `mongodb://127.0.0.1:27017/edupulse` | DB |
| `GEMINI_API_KEY` | *(empty)* | Real Gemini key. Blank = **demo mode** |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Swappable |
| `JWT_SECRET` | dev placeholder | Set a real 32+ char secret in prod |
| `WELLBEING_ENC_KEY` | 64-hex demo | 32-byte AES-256-GCM key for wellbeing notes |
| `PORT` | `5050` | HTTP port |

## API

```
POST /api/auth/login
POST /api/auth/signup        { email, name, password, role, classId?, studentCode?, consent }
GET  /api/auth/me
GET  /api/students?classId=

POST /api/ingest/<attendance|assessments|assignments|engagement|wellbeing>/csv  (multipart, teacher/admin)
POST /api/ingest/attendance                                                      (manual, JSON)

POST /api/predict/{studentId}
GET  /api/latest/{studentId}
GET  /api/class/{classId}
POST /api/support/{studentId}
POST /api/whatif/{studentId}        { deltas: { attendance_rate: 0.1, ... } }
POST /api/interventions/{studentId} { type, notes, status }
GET  /api/interventions/{studentId}
GET  /api/digest/{classId}
POST /api/chat/{studentId}          { message }
GET  /api/chat/{studentId}/history
GET  /api/notifications
POST /api/notifications/{id}/read

GET  /api/admin/fairness            (admin)
GET  /api/admin/metrics             (admin)
GET  /api/features
GET  /api/ai-usage                  (admin via MetaController)
```

## CSV schemas

`studentCode` column resolves each row to a `Student.studentCode` (must exist).

| Endpoint | Required columns |
|----------|------------------|
| `attendance/csv` | `studentCode,date,status,minutesLate?` (status ∈ present/absent/late) |
| `assessments/csv` | `studentCode,subject,exam,marks,maxMarks,date` |
| `assignments/csv` | `studentCode,title,subject,dueDate,submittedAt?,grade?,maxGrade?` |
| `engagement/csv` | `studentCode,weekStart,logins,minutesActive,participationEvents` |
| `wellbeing/csv` | `studentCode,date,mood,stress,sleepHours,notes?` |

## Deploy

* **Frontend** — Vercel (`vercel.json` included). `npm run build` → static SPA.
* **Backend** — Railway (or any Java 21 host). Set the env vars, run `java -jar`.
* **Mongo** — Atlas free tier. Point `MONGO_URI` at it.

`VITE_API_URL` is read by `lib/api.js` automatically if you set it on the
frontend host; locally the Vite proxy handles `/api`.

## Safety

* **Consent** — captured at signup.
* **PII before Gemini** — every Gemini call uses a 12-char pseudonym for the
  student id; raw names/emails never cross the wire.
* **Encryption at rest** — wellbeing free-text encrypted with AES-256-GCM.
* **Human-in-the-loop** — banner copy frames everything as decision-support.
* **No auto-penalising** — admins and mentors do the acting.
* **Bias laundering guard** — `/api/admin/fairness` groups predictions by
  demographic slices and shows avg-risk + high-rate side-by-side.

## Tests

```bash
cd backend
./mvnw test
```

Two test classes:
- `RuleScorerTest` — band boundaries and what-if effect
- `AiServiceTest` — demo-mode shape for explain, plan, chat

## Skipped (deliberate, add later)

* SMTP delivery of weekly digests (stored as in-app notif today).
* Real ML model — current scorer is a transparent weighted sum. Wire in a
  Python microservice later if you want SHAP-style explanations.
* Multi-tenant Redis rate limiter — single-instance only.
