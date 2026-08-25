# PocketWatch — Personal Finance Dashboard

React + Vite frontend for a personal finance tracker: auth, analytics dashboard,
transactions table, CSV upload with progress polling, AI insights, and AI chat.

## Design

Dark "ink" app shell with cream "paper" content cards, brass accent, and
tabular monospace figures for money — a passbook/ledger aesthetic. The
`BackgroundPaths` component on Login/Register is restyled as ambient
"cash flow lines" (brass threads on ink) behind the auth forms.

## Stack

React 18 + Vite · Tailwind CSS · React Router v6 · TanStack Query ·
Axios (JWT interceptor with refresh) · Recharts · React Hook Form ·
Framer Motion · lucide-react

## Setup

```bash
npm install
cp .env.example .env   # point VITE_API_URL at your backend
npm run dev
```

This is wired against the **ET** Spring Boot backend (`/api/v1/...`). Every
response is unwrapped from its `{ success, message, data, timestamp }`
envelope in `src/api/queries.js` via a shared `unwrap()` helper.

- `POST /api/v1/auth/login`, `/register`, `/refresh` (refresh token goes in
  the `Authorization: Bearer <refreshToken>` header, not the body)
- `GET /api/v1/analytics/summary` (current month totals), `/monthly` (chart
  series), `/by-category`
- `GET /api/v1/categories` (used to resolve `categoryId` → name)
- `GET /api/v1/transactions` (0-indexed `page`/`size`/`sort`, filterable by
  `categoryId`, `type`, `search`, `startDate`, `endDate`), `PUT /:id`, `DELETE /:id`
- `POST /api/v1/uploads` (multipart `file`), `GET /:id/status` — status is
  `PENDING → PROCESSING → COMPLETED | FAILED`, no numeric progress field
- `GET /api/v1/ai/insights` (returns a plain string)
- `GET /api/v1/ai/chat/history`, `POST /api/v1/ai/chat`

`accessToken` and `refreshToken` are stored in `localStorage`; the axios
instance in `src/api/axios.js` attaches the access token to every request
and automatically refreshes + retries once on a 401.

**Backend CORS:** the ET `SecurityConfig` had no `CorsConfigurationSource`,
so the browser blocks requests from the Vite dev server by default — add a
CORS bean allowing `http://localhost:5173` before running both together.

## Structure

```
src/
  api/            axios instance + all React Query hooks
  components/     Sidebar, AppLayout, ProtectedRoute, ui/ (button, card, background-paths)
  context/        AuthContext (login/register/logout)
  pages/          Login, Register, Dashboard, Transactions, Upload, Insights, Chat
```

## Notes / next steps

- Wire the edit button on Transactions to a modal or inline form using
  `useUpdateTransaction()` (already implemented in `api/queries.js`).
- Backend response shapes are assumed (e.g. `summary.monthly`,
  `byCategory.categories`, `transactions.items`) — adjust the mapping in
  each page if your API differs.
