# Module 4 — Dashboard & Reports

Fund utilization dashboard + downloadable Excel/PDF summaries, built on top of the
main Subsidy Tracker backend. Currently running on **mock analytics data** until
Module 3 (analytics) is merged — see [Swapping in real data](#swapping-in-real-data-later)
below.

## What's in this module

- `com.subsidytracker.dashboard` — `AnalyticsDataSource` interface, a temporary
  `MockAnalyticsDataSource`, DTOs, and `DashboardController` (`/api/v1/dashboard/*`).
- `com.subsidytracker.reports` — `ReportService` (Apache POI for Excel, OpenPDF for
  PDF) and `ReportController` (`/api/v1/reports/*`), serving downloadable files.
- `src/main/resources/static/dashboard/index.html` — the dashboard page itself.
- One shared-file change: `SecurityConfig.java` has an added `permitAll()` rule so
  the static dashboard page can load without auth (the API calls it makes still
  require a JWT).

## Prerequisites

- **Java 17**
- **Maven** (or the included `./mvnw`)
- **MySQL 8.0.x**, running locally
- Internet access on first build, so Maven can download the POI/OpenPDF
  dependencies from Maven Central

## First-time setup

### 1. Clone and branch
```bash
git clone https://github.com/Harshith-A-N/subsidy-tracker.git
cd subsidy-tracker
git checkout dev
git pull origin dev
```

### 2. Create your local DB config
This file is **git-ignored on purpose** — everyone creates their own with their own
MySQL credentials. Create:

```
src/main/resources/application-local.properties
```

with:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/subsidy_tracker_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

MySQL must already be running — `createDatabaseIfNotExist=true` handles creating
the schema itself.

### 3. Run it
```bash
./mvnw spring-boot:run
```
Watch the top of the log: it should say **"Copying 2 resources"** (confirming your
local config was picked up), and end with `Started SubsidyTrackerApplication...`.

## Getting a login token

The dashboard needs a logged-in JWT. Using Postman (or curl):

**Register** — `POST http://localhost:8080/api/v1/auth/register`
```json
{
  "fullName": "Your Name",
  "email": "you@test.com",
  "password": "test1234"
}
```

**Login** — `POST http://localhost:8080/api/v1/auth/login`
```json
{
  "email": "you@test.com",
  "password": "test1234"
}
```
Copy the `token` field from the response.

## Viewing the dashboard

Open `http://localhost:8080/dashboard/index.html`, paste the token into the box at
the top, click **Load Dashboard**. You should see stat cards, fund-utilization
charts by scheme/region, a category-distribution chart, a budget-exhaustion
warnings table, a non-compliance table, and four report download buttons.

## Report downloads

The four buttons on the dashboard page download real files:
- Scheme Summary — Excel (`.xlsx`) / PDF
- Regional Summary — Excel (`.xlsx`) / PDF

Or hit the endpoints directly (with `Authorization: Bearer <token>` header):
```
GET /api/v1/reports/schemes/excel
GET /api/v1/reports/schemes/pdf
GET /api/v1/reports/regions/excel
GET /api/v1/reports/regions/pdf
```

## Swapping in real data later

Once Module 3 (`com.subsidytracker.analytics`) is merged with real
implementations:
1. Implement `AnalyticsDataSource` for real — either directly in the analytics
   package, or as a small adapter class that calls Person 3's `AnalyticsService`.
2. Annotate that new bean `@Primary`.
3. Delete `MockAnalyticsDataSource`, or remove its `@Service` annotation, so Spring
   doesn't see two competing beans.

Nothing in `DashboardController`, `ReportService`, or `dashboard/index.html` needs
to change — they only depend on the `AnalyticsDataSource` interface.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Failed to configure a DataSource` on startup | `application-local.properties` is missing or in the wrong folder. It must sit directly in `src/main/resources/`, and the resource-copy log line should read "Copying 2 resources", not 1. |
| `package com.lowagie.text does not exist` / POI class not found at compile time | Your `pom.xml` is missing the POI/OpenPDF `<dependency>` blocks — check they're present inside `<dependencies>...</dependencies>`, then rerun. Needs internet access to Maven Central on first build. |
| `Column 'region' cannot be null` or `Data truncated for column 'role'` on register | Stale local DB schema from before some column/enum was widened. Fix with: `DROP DATABASE IF EXISTS subsidy_tracker_db;` then restart the app to let Hibernate recreate it, or `ALTER TABLE users MODIFY role ENUM(...) NOT NULL;` / `ALTER TABLE users MODIFY region VARCHAR(255) NULL;` to patch in place. |
| Dashboard page loads but charts stay empty / 401 errors | Token missing, expired, or not pasted into the box. Re-login and re-paste. |
| Register request returns `full_name` null even though you sent it | In Postman, Body tab → raw → make sure the dropdown is set to **JSON**, not Text, so `Content-Type: application/json` actually gets sent. |
