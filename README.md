# Government Subsidy/Grant Disbursement Tracking System

A Java/Spring Boot application to streamline the government subsidy and grant disbursement lifecycle — from beneficiary application through eligibility scoring, multi-level verification, staged disbursement, and fund utilization analytics.

## Problem Statement

Traditional subsidy/grant disbursement processes involve manual eligibility checks, fragmented verification records, inconsistent approval standards across regions, and limited visibility into fund utilization — leading to delays, leakage risk, and poor transparency. This system automates eligibility scoring, routes applications through multi-level verification, manages staged disbursement tied to compliance milestones, and provides fund utilization analytics by scheme and region.

## Tech Stack

- Java 17
- Spring Boot (Spring Web, Spring Data JPA, Spring Security, Validation)
- MySQL 8.0.x
- Maven
- Lombok

## Modules

| # | Module
|---|
| 1 | Beneficiary & Scheme Master Data Management
| 2 | Eligibility Scoring & Multi-Level Verification Workflow Engine
| 3 | Staged Disbursement & Compliance Milestone Tracking
| 4 | Fund Utilization & Regional Analytics
| 5 | Security, Integration & Deployment

## Data Model (shared foundation)

- **User** — system users (field officer, district officer, finance approver, admin) — role-based, no self-registration
- **Beneficiary** — the person a scheme benefits; does not log in (staff-mediated model)
- **Scheme** — a government grant/subsidy program; eligibility criteria live here
- **SchemeSlab** — grant amount for a scheme, per beneficiary category
- **RegionalBudget** — allocated vs. utilized budget for a scheme, per region
- **Application** — the event of a Beneficiary applying to a Scheme; carries status (`ApplicationStatus`), eligibility score, and submission date. This is the central entity Modules 2 and 3 build on.

## Local Setup

1. Clone the repo and check out `dev`:
   ```bash
   git clone <repo-url>
   cd subsidy-tracker
   git checkout dev
   git pull origin dev
   ```
2. Install MySQL 8.0.x locally and create the database:
   ```sql
   CREATE DATABASE subsidy_tracker_db;
   ```
3. Create your own `src/main/resources/application-local.properties` (this file is git-ignored ):
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/subsidy_tracker_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
   spring.datasource.username=YOUR_MYSQL_USERNAME
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
   ```
4. Run `SubsidyTrackerApplication.java`. Confirm it starts with no errors.
5. Verify tables were created:
   ```sql
   USE subsidy_tracker_db;
   SHOW TABLES;
   ```
   You should see: `users`, `beneficiaries`, `schemes`, `scheme_slabs`, `regional_budgets`, `applications`.

## Git Workflow

- `main` — stable, milestone checkpoints only
- `dev` — integration branch; all feature work merges here first
- `feature/<module>-<description>` — one branch per task, branched off `dev`

**Standard loop for any task:**
```bash
git checkout dev
git pull origin dev
git checkout -b feature/<your-branch-name>

# ...do your work...

git add .
git commit -m "short description"
git push -u origin feature/<your-branch-name>
```
Open a PR on GitHub with **base: `dev`**, get 1 approval, merge, then everyone pulls `dev` again before starting their next task.

## Current Progress

See [`docs/progress.md`](docs/progress.md) for the latest status and task assignments.

