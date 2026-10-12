# EduPulse — Backend (Spring Boot + MongoDB + Gemini)

Decision-support platform for student academic risk. Single in-process
rule-based scoring (kept small). All AI goes through one `AiService` class
with a **demo mode** (deterministic mocks) whenever `GEMINI_API_KEY` is
blank — paste a real key in `.env` to flip to live Gemini.

The frontend lives in a separate repo: `padarthi22/edupulse-frontend`.

## Run locally

```bash
docker run -d -p 27017:27017 mongo:7   # or use your MONGO_URI
cp .env.example .env
mvn spring-boot:run                    # http://localhost:5050
# Seed demo data on first boot:
EDUPULSE_SEED=true mvn spring-boot:run
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
POST /api/auth/signup
GET  /api/auth/me
GET  /api/students?classId=

POST /api/ingest/<attendance|assessments|assignments|engagement|wellbeing>/csv
POST /api/ingest/attendance

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
GET  /api/ai-usage
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

```bash
railway init
railway up    # uses the included Dockerfile
```

Then set env vars (`MONGO_URI`, `GEMINI_API_KEY`, `JWT_SECRET`,
`WELLBEING_ENC_KEY`, `GEMINI_MODEL`) in the Railway dashboard.

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
mvn test
```

Two test classes:
- `RuleScorerTest` — band boundaries and what-if effect
- `AiServiceTest` — demo-mode shape for explain, plan, chat

## Skipped (deliberate, add later)

* SMTP delivery of weekly digests (stored as in-app notif today).
* Real ML model — current scorer is a transparent weighted sum.
* Multi-tenant Redis rate limiter — single-instance only.
