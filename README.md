# Smart Expense & Budget Tracker

A single-user personal finance application served by Spring Boot. It uses Java 21, Maven, PostgreSQL, Flyway, HTML, CSS, and browser-native JavaScript. No Node.js, npm, frontend build step, AI APIs, AI keys, sample transactions, or hardcoded exchange rates are required. The existing static UI is served by the Java application.

This repository was migrated from its prior JavaScript backend to Java. The supported application source is under `src/main/java` and `src/main/resources`; the old `backend/` folder is no longer part of the application.

## Requirements

- Java 21 (JDK)
- Maven 3.6.3 or newer
- PostgreSQL 15 or newer, or Docker Compose

## Start locally

1. Start a local PostgreSQL database. If Docker Compose is installed, run `docker compose up -d` from the repository root. Otherwise create a database named `expense_tracker` and a user with access to it.
2. Copy `.env.example` to `.env` and set `DATABASE_URL`, `DB_USERNAME`, and `DB_PASSWORD` for your local database. The application does not load `.env` automatically; export those variables in the shell or configure them in your IDE/run configuration. Spring defaults target a local development database.
3. From this folder, run `mvn spring-boot:run`.
4. Open [http://localhost:8080](http://localhost:8080).

Flyway applies `src/main/resources/db/migration/V1__initial_schema.sql` at startup. Hibernate validates the schema and does not create or alter tables. PostgreSQL stores all finance and activity data across restarts. Do not expose this single-user application publicly without adding authentication and access controls.

PowerShell example for environment variables:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/expense_tracker"
$env:DB_USERNAME = "expense"
$env:DB_PASSWORD = "your-local-password"
mvn spring-boot:run
```

## API

JSON monetary values are represented as decimal values. Transaction and budget writes validate positive amounts, supported categories, required income/expense details, and date/period ranges. List pages are one-based. Filters are optional.

| Method and path | Purpose |
| --- | --- |
| `GET /api/health` | Application and PostgreSQL readiness |
| `GET /api/categories` | Expense categories |
| `GET /api/transactions?page=1&pageSize=20` | Paged transactions; supports `type`, `category`, `from`, `to`, `q`, `paymentMode`, `incomeSource` |
| `GET /api/transactions/{id}` | Read a transaction |
| `POST /api/transactions` | Create an income or expense |
| `PUT /api/transactions/{id}` | Replace a transaction |
| `DELETE /api/transactions/{id}` | Delete a transaction |
| `GET /api/budgets?month=1&year=2026` | Monthly budgets, spend and status |
| `GET /api/budgets/{id}` | Read a budget |
| `POST /api/budgets` | Create a category budget |
| `PUT /api/budgets/{id}` | Update a budget |
| `DELETE /api/budgets/{id}` | Delete a budget |
| `GET /api/dashboard/summary?month=1&year=2026` | Monthly totals and count |
| `GET /api/dashboard/category-spending?month=1&year=2026` | Category expense totals |
| `GET /api/dashboard/monthly-summary` | Monthly income and expense totals |
| `GET /api/dashboard/budget-status?month=1&year=2026` | Budget status and threshold flags |
| `GET /api/dashboard/recent-transactions` | Latest transactions |
| `GET /api/activity?limit=20` | Persisted activity history |
| `GET /api/activity/{id}` | Read an activity entry |
| `GET /api/currency/rates?base=INR&date=2026-10-21` | Latest or historical Frankfurter rates |
| `GET /api/currency/convert?amount=100&from=INR&to=USD` | Decimal-safe conversion via Frankfurter |

Currency conversion uses the official no-key Frankfurter v2 API (`/rates?base=INR`, `/rate/{base}/{quote}`, optional `date`). Responses identify provider, rate date, cached state, and stale-cache fallback. Latest responses are cached for six hours. If an external request fails and an expired response is available, the response identifies it as cached and stale; otherwise the currency endpoint returns an error. Transaction and budget features do not depend on Frankfurter. Conversion does not change stored transactions.

## Tests and build

Run `mvn test` and `mvn package`. Current JUnit/Mockito unit tests cover expense creation and validation, dashboard aggregation, budget thresholds, currency decimal conversion, caching, malformed provider data, and provider failure. No PostgreSQL integration or browser automation tests have been implemented yet. No Node runtime or frontend compilation is used. The application cannot start fully without a reachable PostgreSQL database because PostgreSQL is the persistent store.

## Data and security notes

- Database access uses JPA with parameterized queries. Schema and seed categories are managed by Flyway.
- Database settings are configured with `DATABASE_URL`, `DB_USERNAME`, and `DB_PASSWORD`; never commit real credentials.
- There are no user accounts or authentication. Treat this as a private local instance only.
- There is no analytics integration. Transaction records and activity events are stored in PostgreSQL until manually deleted; deleting a transaction does not erase its activity history.
- Privacy and Terms pages are available at `/privacy.html` and `/terms.html`. They are project-level informational drafts, not legal advice. Review and adapt them to the actual operator, hosting, retention, and applicable law before launch.

## Launch checklist

**NOT READY FOR PUBLIC LAUNCH.** This repository has not been deployed, and no domain or HTTPS endpoint has been configured or verified. The favicon and legal-information pages are included, but should be verified in the deployed environment and the legal drafts reviewed. Authentication and authorization are not implemented; do not expose personal financial records publicly. Production backup, monitoring, and hosting configuration also remain deployment responsibilities.

To stop the optional local database, use `docker compose down`. Keep its named volume unless you intentionally want to remove local data.
