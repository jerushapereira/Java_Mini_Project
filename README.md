# Smart Expense & Budget Tracker

A self-hosted personal finance application with a small browser interface, a Node.js REST API, PostgreSQL persistence, and live exchange rates from Frankfurter. This is a new standalone project. No source repository or existing frontend was provided, so its interface is newly authored rather than represented as a preserved design.

The application has no AI integrations, AI-generated responses, API keys, sample financial records, or hardcoded exchange rates. It is a single-user local workspace without authentication; do not expose it to an untrusted network without adding access controls.

## Requirements

- Node.js 20 or newer
- PostgreSQL 15 or newer
- Docker Compose is optional for local PostgreSQL

## Run locally

1. From this project directory, start PostgreSQL with `docker compose up -d`. Alternatively, point `DATABASE_URL` at an existing PostgreSQL instance.
2. Copy `.env.example` to `.env`. Set `DATABASE_URL` to match the database. The example credentials are for local development only.
3. Install dependencies with `npm install`.
4. Create the tables with `npm run migrate`. The initial migration is `backend/migrations/001_initial.sql`.
5. Start the server with `npm start` and open `http://localhost:3000`.

The Express server serves the frontend and API from the same origin. `FRONTEND_ORIGIN` limits cross-origin API requests when a separate frontend origin is used. `DATABASE_SSL=true` enables certificate-verified TLS for PostgreSQL. No currency API key or paid subscription is needed.

To stop the local database, use `docker compose down`. Its named volume keeps data between restarts. Remove the volume only when you intentionally want to delete local data.

## Tests

Run `npm test`. Tests exercise decimal-safe currency multiplication, rounding, provider response caching, currency validation, and provider outage handling. These tests do not need PostgreSQL or internet access. Database/API integration tests were not run as part of this source scaffold; run them against a configured PostgreSQL instance before production use.

## API

All endpoints use JSON except `204 No Content` deletes. Monetary inputs use decimal strings and are persisted as PostgreSQL `NUMERIC(14,2)`. Transaction list responses use `{ items, page, pageSize, total }`, ordered by transaction date and ID descending. List filters include `type`, `category`, `from`, `to`, `month`, `year`, `q`, `paymentMode`, and `incomeSource`.

| Method and path | Purpose |
| --- | --- |
| `GET /api/health` | Database readiness |
| `GET /api/transactions` | Paged transaction list with filters |
| `GET /api/transactions/:id` | Read one transaction |
| `POST /api/transactions` | Create income or expense |
| `PUT /api/transactions/:id` | Replace transaction fields |
| `DELETE /api/transactions/:id` | Delete transaction |
| `GET /api/budgets?month=&year=` | List monthly budgets with spending status |
| `GET /api/budgets/:id` | Read one budget with spending status |
| `POST /api/budgets` | Create a category budget |
| `PUT /api/budgets/:id` | Update a category budget |
| `DELETE /api/budgets/:id` | Delete a category budget |
| `GET /api/dashboard/summary?month=&year=` | Monthly totals, category totals, balance, count |
| `GET /api/dashboard/category-spending?month=&year=` | Expense totals by category |
| `GET /api/dashboard/monthly-summary` | Up to 12 months of income and expenses |
| `GET /api/dashboard/budget-status?month=&year=` | Budget use, remaining amount, warning and exceeded flags |
| `GET /api/dashboard/recent-transactions` | Latest 10 transactions |
| `GET /api/activity?limit=` | Recent persisted activity |
| `GET /api/activity/:id` | Read one activity record |
| `GET /api/currency/rates?base=INR&date=YYYY-MM-DD` | Real provider rates for a base currency, optional historical date |
| `GET /api/currency/convert?amount=1000&from=INR&to=USD` | Convert using a provider rate; optional `date` |

Transactions require positive amounts, an ISO date, and a valid type. Expenses require one of Food, Travel, Shopping, Education, or Entertainment plus a payment mode. Income requires an income source and does not enter expense-category totals. Budgets are unique per category, month, and year. At least 80% use sets `warning`; spending above the limit sets `exceeded`.

## Currency data

The backend `CurrencyService` calls Frankfurter v2. It uses `/v2/rates?base=INR` for current rates, `/v2/rate/INR/USD` for a pair, and the documented `date` parameter for historical rates such as `2026-10-21`. The provider documents an unauthenticated API and a flat list of rate records; currency availability is determined by provider responses. Pair and base responses are cached in process for six hours. Responses say whether data is cached; an expired cached result is marked `stale` if refreshing fails. With no cached result, provider timeouts or outages return an error rather than a fabricated rate. Conversion is performed with integer decimal arithmetic and rounded to cents.

Frankfurter documentation: [API documentation](https://frankfurter.dev/) and [v2 JavaScript examples](https://frankfurter.dev/javascript/).

## Data and deployment notes

- PostgreSQL migrations create transaction, budget, and activity tables with checks, unique constraints, indexes, and foreign keys.
- Transaction create/update/delete and budget changes write activity rows in the same database transaction.
- There is no authentication or user separation. Keep the service bound to a trusted local network until authentication and authorization are added.
- The included privacy and terms pages describe the behavior of this starter. Update them for the actual operator, jurisdiction, retention policy, and deployment before public launch.
- A favicon is included. No custom domain is configured. This project is self-hosted and does not include a platform-generated “Made with AI” badge.
- No existing website was provided, so this deliverable does not claim to preserve an existing frontend or to remove a badge from one.
