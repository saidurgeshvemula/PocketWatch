# PocketWatch

A personal finance tracker — upload bank statements, get transactions automatically
categorized, track spending across accounts, and get AI-powered insights and chat
about your own financial data.

**Live app:** https://pocket-watch-orpin.vercel.app

## Monorepo structure

```
PocketWatch/
├── PocketWatch-Backend/   Spring Boot 3.5 API (Java 21)
└── PocketWatch-Frontend/  React 18 + Vite SPA
```

## Features

### Auth & accounts
- Email/password registration with mandatory email verification
- JWT auth (access + refresh tokens), silent refresh on the frontend
- Password reset via emailed, time-limited tokens
- Protected routes on the frontend, Spring Security-enforced authorization on the backend

### Transactions
- Manual transaction entry, plus a dedicated cash-expense flow
- Multiple bank account management
- Bulk delete by date range, by month, or by ID list
- Full transaction history with filtering

### Statement upload & parsing
- Upload bank statements as CSV or PDF
- PDF text extraction via Apache PDFBox and Apache Tika
- Normalization layer that maps inconsistent bank-specific formats (dates, columns,
  merchant naming) into one consistent internal schema
- PII masking applied to extracted data before storage
- Async processing with pollable upload status, so large files don't block the request

### Automatic categorization
- Rule engine matching transactions to categories via prioritized keyword/merchant rules
- AI categorization fallback (Groq LLM API) for anything the rules can't confidently classify
- Merchant name normalization and alias lookup

### Analytics
- Monthly income/spend summaries, category breakdowns, top-merchant summaries
- Visualized on the frontend with Recharts

### AI features
- Chat interface grounded in the user's own transaction data
- AI-generated plain-language financial insight summaries

## Tech stack

**Backend**
- Java 21, Spring Boot 3.5 (Web, Security, Data JPA, Validation, Actuator)
- MySQL 8/9, Flyway migrations, Hibernate/JPA
- JWT auth (jjwt), Apache PDFBox / Tika for statement parsing
- Groq API (OpenAI-compatible) for AI categorization, insights, and chat
- SendGrid for transactional email (verification, password reset)
- Docker (multi-stage build, non-root runtime user)

**Frontend**
- React 18 + Vite, Tailwind CSS
- React Router v6, TanStack Query, Axios (JWT refresh interceptor)
- Recharts for analytics charts, Framer Motion for animation
- Dark "ledger" aesthetic — ink background, cream content cards, brass accents

## Local development

### Backend

```bash
cd PocketWatch-Backend
cp .env.example .env        # fill in DB, JWT, Groq, SendGrid values
docker-compose up -d        # starts local MySQL only
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Runs on `http://localhost:8080`. Swagger UI at `/swagger-ui.html`.

### Frontend

```bash
cd PocketWatch-Frontend
npm install
cp .env.example .env         # set VITE_API_URL=http://localhost:8080/api/v1
npm run dev
```

Runs on `http://localhost:5173`.

## Deployment

- **Backend** → Railway (Docker build from `PocketWatch-Backend/`, root directory set
  in Railway service settings) + Railway-managed MySQL, same project.
- **Frontend** → Vercel (root directory set to `PocketWatch-Frontend/`), auto-builds
  with Vite on every push to `main`.
- Both platforms auto-deploy on push to `main`. Environment variables are set in each
  platform's dashboard — never committed to the repo.

### Backend environment variables (Railway)

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL / USERNAME / PASSWORD   (from Railway MySQL service references)
JWT_SECRET
OPENAI_API_KEY          (Groq API key)
SENDGRID_API_KEY
SENDGRID_FROM_ADDRESS   (a SendGrid-verified sender)
SENDGRID_FROM_NAME
CORS_ALLOWED_ORIGINS    (live frontend URL)
FRONTEND_URL            (live frontend URL, used in email links)
```

### Frontend environment variables (Vercel)

```
VITE_API_URL   (live backend URL + /api/v1)
```

## Database migrations

Flyway migrations live in `PocketWatch-Backend/src/main/resources/db/migration/`.
They run automatically on application startup, in order, and are never re-run once applied.

## Security notes

- Secrets (DB passwords, API keys, JWT secret) are never committed — only referenced by
  name in `.env.example`. Real values live in Railway/Vercel dashboards or a local
  untracked `.env`.
- Email sending uses SendGrid's Single Sender Verification rather than Gmail SMTP,
  since most cloud hosts (including Railway) block outbound SMTP ports by default.
- Dependencies are periodically checked for known CVEs (e.g. Apache Tika was upgraded
  after a critical XXE vulnerability was found in the previously pinned version).
