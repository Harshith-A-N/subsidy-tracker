# Subsidy Tracker

**Development of Digital Subsidy and Grant Administration Platform**

🌐 **Live Demo:** [https://digital-subsidy-platform.web.app/](https://digital-subsidy-platform.web.app/)

A Java/Spring Boot backend that manages the full lifecycle of a government subsidy or grant — from scheme configuration and beneficiary application, through multi-level eligibility verification, to staged fund disbursement tied to compliance milestones, and regional fund-utilization analytics.

---

## Table of Contents

1. [Overview](#overview)
2. [Key Features](#key-features)
3. [User Roles & Demo Credentials](#user-roles--demo-credentials)
4. [End-to-End Workflow](#end-to-end-workflow)
5. [Staged Disbursement Model](#staged-disbursement-model)
6. [Security & Authorization](#security--authorization)
7. [Document Management & In-App Viewer](#document-management--in-app-viewer)
8. [Audit Logging](#audit-logging)
9. [External Integrations](#external-integrations)
10. [Database / Domain Model](#database--domain-model)
11. [Technology Stack](#technology-stack)
12. [Project Structure](#project-structure)
13. [Backend Architecture](#backend-architecture)
14. [API Overview](#api-overview)
15. [Application Status Flow](#application-status-flow)
16. [Disbursement & Compliance Statuses](#disbursement--compliance-statuses)
17. [Configuration](#configuration)
18. [Installation & Setup](#installation--setup)
19. [Design / Business Rules](#design--business-rules)

---

## Overview

Traditional subsidy and grant disbursement processes are typically manual: eligibility is checked by hand, verification records are fragmented across offices, approval standards vary between regions, and there is little visibility into how allocated funds are actually being used. This leads to delays, leakage risk, and poor transparency.

**Subsidy Tracker** is a full-stack digital governance platform that digitizes this process end-to-end:

- Beneficiaries apply for government schemes, track application status, and view real-time disbursement schedules.
- Eligibility is calculated automatically against a scheme's income and category rules.
- Applications move through a **three-level verification chain** — Field Officer → District Officer → Finance Approver — with region-based routing.
- Approved applications receive an automatically generated **staged disbursement schedule**, where each stage releases funds only after the previous stage's utilization is verified.
- An interactive in-app document viewer enables officers and citizens to inspect KYC proofs and utilization reports directly with zoom and rotation tools.
- Administrators configure schemes, grant slabs, regional budgets, and disbursement plans.
- District Officers, Finance Approvers, and Admins get consolidated analytics (fund utilization by scheme/region, non-compliance, budget exhaustion warnings) and downloadable Excel/PDF reports.

The system is architected as a Spring Boot 3 backend paired with a modern React + Vite single-page application, backed by **Neon Serverless PostgreSQL** (PostgreSQL 16) in cloud production and local PostgreSQL or H2 at runtime/test scope. Document storage is powered by **Cloudinary CDN** with an interactive in-app document viewer.

---

## Key Features

### Authentication & Security
- JWT-based authentication (stateless, no server-side sessions)
- BCrypt password hashing
- Role-based access control enforced centrally in `SecurityConfig`
- Server-resolved identity for all state-changing actions (client-supplied user/officer IDs are never trusted)
- Beneficiary ownership checks on applications and documents
- Region-based authorization for Field/District Officers; statewide access for Finance Approvers and Admins

### Beneficiary Management
- Self-registration (`BENEFICIARY` role is server-assigned)
- One beneficiary profile per user account
- Profile fields: category, region, annual income, contact details
- Scheme browsing (active schemes only, unless Admin)

### Application Lifecycle
- Draft-first application creation, followed by document upload, then formal submission
- Automatic eligibility scoring on submission
- Multi-level verification workflow with fast-track routing for low-risk applications
- Re-verification routing back to an earlier stage
- Application status history is fully attributable via `Verification` records

### Eligibility Engine
- Income-limit and category checks against `Scheme` configuration
- Missing-field-officer-in-region check (routes to manual review)
- Weighted eligibility score (income ratio + profile completeness)

### Staged Disbursement
- One `DisbursementPlan` per scheme, made up of ordered `DisbursementStage`s
- Automatic schedule generation once an application reaches `READY_FOR_DISBURSEMENT`
- Sequential, finance-controlled stage release (a stage cannot release before the previous stage's compliance is verified)
- Stage-specific utilization-proof upload and officer compliance verification
- Automatic application status advancement to `DISBURSED` / `COMPLETED`
- Scheduled daily job to flag overdue compliance milestones

### Administration
- Scheme, slab, and regional budget configuration
- Disbursement plan and stage configuration
- Officer self-registration with Admin approval workflow
- User listing for administrative oversight

### Analytics & Reporting
- Dashboard overview rollup (applications, budgets, overdue milestones)
- Fund utilization by scheme and by region
- Pending/overdue compliance milestone summary
- Non-compliance analysis by scheme and region
- Approval turnaround time (submission → Finance approval)
- Budget exhaustion warnings (OK / WARNING / CRITICAL)
- Beneficiary category distribution
- Downloadable Excel (Apache POI) and PDF (OpenPDF) scheme/region summary reports

---

## User Roles & Demo Credentials

| Role | Responsibilities | Assigned Scope |
|---|---|---|
| **ADMIN** | Creates and updates schemes, scheme slabs, and regional budgets; configures disbursement plans and stages; approves/rejects officer registration requests; can view all schemes (including inactive) and all applications; can manually trigger eligibility recalculation; can complete compliance milestones. | Statewide / Global |
| **BENEFICIARY** | Registers an account and creates a beneficiary profile; browses active schemes; creates draft applications; uploads KYC documents and, later, stage-specific utilization proofs; formally submits applications for eligibility evaluation; views only their own applications, documents, and disbursement schedule. | Individual Citizen |
| **FIELD_OFFICER** | Verifies KYC documents for applications in their assigned region; approves, rejects, or requests re-verification at the Field stage; verifies utilization-proof documents and completes compliance milestones for applications in their region. | Region-Specific (`Maharashtra`) |
| **DISTRICT_OFFICER** | Reviews applications that have passed Field verification, in their assigned region; approves, rejects, or requests re-verification; can complete compliance milestones in their region. | Region-Specific (`Maharashtra`) |
| **FINANCE_APPROVER** | Reviews applications statewide (no regional restriction) after District approval; gives final approval, which triggers automatic disbursement-schedule generation; releases each disbursement stage in sequence; **cannot** complete compliance milestones (separation of duties from fund verification). | Statewide / Global |

Officer accounts (`FIELD_OFFICER`, `DISTRICT_OFFICER`, `FINANCE_APPROVER`) are not self-registered directly — a request is submitted via `/api/v1/auth/officer-register` and must be approved by an Admin before the account is created.

### Pre-Configured Demo Accounts

For platform demonstration, functional evaluation, and local development, the database includes pre-configured accounts across all administrative tiers and citizen profiles (default password: `123456`):

| Role | Email | Password | Assigned Region | Access & Capabilities |
|---|---|---|---|---|
| **System Administrator** | `admin@gmail.com` | `123456` | Statewide (`ALL`) | Full scheme, budget, slab, and officer request governance |
| **Finance Approver** | `fa@gmail.com` | `123456` | Statewide (`All Regions`) | Final sanction, tranche release, treasury disbursement simulation |
| **District Officer** | `do@gmail.com` | `123456` | `Maharashtra` | Stage 2 regional review, field officer oversight, milestone signoff |
| **Field Officer** | `fo@gmail.com` | `123456` | `Maharashtra` | Ground-level inspection, in-app KYC verification, field approval |
| **Demo Beneficiary (Citizen)** | `me@gmail.com` | `123456` | `Maharashtra` | Demo citizen applicant (`GENERAL`, ₹1.5L income) with active grants |

> [!NOTE]
> Additional diverse beneficiaries are also seeded across social categories (`OBC`, `SC`, `ST`, `EWS`) and states (`Uttar Pradesh`, `Gujarat`, `Karnataka`, `Rajasthan`) such as `ramesh@gmail.com`, `sunita@gmail.com`, and `suresh@gmail.com` (all using password `123456`).

---

## End-to-End Workflow

```mermaid
flowchart TD
    A[Admin configures Scheme, Slabs, Regional Budgets, Disbursement Plan] --> B[Beneficiary registers and creates profile]
    B --> C[Beneficiary creates draft Application and uploads KYC documents]
    C --> D[Beneficiary submits Application]
    D --> E{Eligibility Check}
    E -->|Income/category fails| F[NOT_ELIGIBLE]
    E -->|No Field Officer in region| G[MANUAL_REVIEW_REQUIRED]
    E -->|Passes| H[FIELD_VERIFICATION_PENDING]
    H --> I[Field Officer verifies KYC docs and approves]
    I -->|Fast-track eligible| K[FINANCE_REVIEW_PENDING]
    I -->|Standard path| J[DISTRICT_REVIEW_PENDING]
    J --> K[District Officer approves]
    K --> L[Finance Approver approves]
    L --> M[READY_FOR_DISBURSEMENT: schedule auto-generated]
    M --> N[Finance releases Stage 1]
    N --> O[Beneficiary uploads utilization proof for the stage]
    O --> P[Field/District Officer verifies proof, completes milestone]
    P --> Q{More stages?}
    Q -->|Yes| N
    Q -->|No| R[Application status: DISBURSED / COMPLETED]
```

### Step 1 — Admin Configures the Scheme
The Admin creates a `Scheme` (name, description, min/max income, comma-separated allowed categories, comma-separated required documents). The Admin then attaches one `SchemeSlab` per beneficiary category (defines the grant amount for that category) and one `RegionalBudget` per region (allocated budget for the scheme in that region). Finally, the Admin configures a single `DisbursementPlan` for the scheme, made up of ordered `DisbursementStage`s (each with a percentage of the grant, a trigger milestone, and a due-date offset). Stage percentages must total exactly 100%.

### Step 2 — Beneficiary Applies
A beneficiary registers (`POST /api/v1/auth/register`), creates a beneficiary profile (`POST /api/v1/beneficiaries`), and creates a draft application for a chosen active scheme (`POST /api/v1/applications`, status `DRAFT`). Required KYC documents (from `Scheme.requiredDocuments`) are uploaded via Cloudinary-backed file storage while the application is `DRAFT` or `RE_VERIFICATION_REQUIRED`.

### Step 3 — Submission & Eligibility
When the beneficiary calls `POST /api/v1/applications/{id}/submit`, the system first checks that every required document has been uploaded. If any are missing, submission is rejected and the application stays `DRAFT`. Otherwise, `EligibilityService` runs:
1. Income check against `Scheme.maxIncome`.
2. Category check against `Scheme.allowedCategories`.
3. A check that at least one `FIELD_OFFICER` is registered for the beneficiary's region.
4. If all checks pass, a weighted eligibility score is calculated and the application becomes `FIELD_VERIFICATION_PENDING`.

### Step 4 — Field Verification
A Field Officer whose `region` matches the beneficiary's region reviews the application. Every KYC document must be individually marked `VERIFIED` (`PATCH /documents/{documentId}/verify`) before the officer can approve. On approval, the routing engine checks whether the application qualifies for **fast-track** (eligibility score ≥ 80 and the applicable `SchemeSlab.grantAmount` ≤ 50,000) — if so, it skips District review and goes straight to `FINANCE_REVIEW_PENDING`; otherwise it goes to `DISTRICT_REVIEW_PENDING`.

### Step 5 — District Review
A District Officer whose region matches the beneficiary reviews the Field Officer's decision and either approves (→ `FINANCE_REVIEW_PENDING`), rejects (→ `DISTRICT_REJECTED`), or requests re-verification (→ back to `FIELD_VERIFICATION_PENDING`).

### Step 6 — Finance Approval
A Finance Approver (statewide, no region restriction) gives the final approval. This moves the application to `READY_FOR_DISBURSEMENT` and, in the same transaction, automatically triggers `ScheduleGenerationService.generateSchedule()`.

### Step 7 — Schedule Generation
The grant amount is resolved from the `SchemeSlab` matching the scheme and the beneficiary's category. For each ordered `DisbursementStage`, an `ApplicationDisbursementSchedule` row is created with `scheduledAmount = grantAmount × stage.percentageOfGrant / 100` and a staggered due date. A `DisbursementMilestone` is created for every schedule entry (status `PENDING` / `NOT_RELEASED`).

### Step 8 — Stage Release
A Finance Approver or Admin releases a stage (`POST /api/disbursement/schedules/{scheduleId}/release`). The first stage can be released immediately; every subsequent stage requires the **previous** stage to already be `RELEASED` **and** its corresponding milestone to already be `COMPLETED`. Releasing a stage also adds the released amount to the matching `RegionalBudget.utilizedBudget`.

### Step 9 — Utilization Proof
Once a stage is released, the beneficiary uploads a stage-linked utilization proof document (`stageId` supplied on upload). This sets the corresponding milestone's `complianceStatus` to `PROOF_SUBMITTED`.

### Step 10 — Compliance Verification
A Field or District Officer whose region matches the beneficiary (or an Admin) verifies the proof and completes the milestone (`PUT /api/disbursement/compliance/{milestoneId}/complete`). This requires the stage to already be `RELEASED` and a proof document to exist. **Finance Approvers are explicitly barred** from completing milestones, keeping fund release and compliance verification as separate duties.

### Step 11 — Next Stage / Completion
Completing a milestone unlocks release of the next stage (Step 8 repeats). Once every stage's schedule is `RELEASED`, the application status automatically advances: to `COMPLETED` if the final stage's trigger milestone is `PROJECT_CLOSURE`, otherwise to `DISBURSED`.

A daily scheduled job (`@Scheduled`, 1:00 AM) automatically flags any `PENDING` milestone whose due date has passed as `OVERDUE`.

---

## Staged Disbursement Model

Disbursement is never a single lump-sum payment. It is split into ordered stages, each tied to a compliance condition.

| Concept | Entity | Meaning |
|---|---|---|
| **Disbursement Plan** | `DisbursementPlan` | One per `Scheme`. Defines how many stages exist and who created the plan. |
| **Disbursement Stage** | `DisbursementStage` | A single configured stage of the plan: name, sequence number, percentage of the total grant, trigger milestone, and due-date offset. Percentages across all stages of a plan must total 100%. |
| **Schedule Entry** | `ApplicationDisbursementSchedule` | A concrete, per-application instantiation of a stage: the actual `scheduledAmount` in currency and a real `dueDate`. Tracks whether the **money** has moved (`DisbursementScheduleStatus`: `PENDING`, `RELEASED`, `ON_HOLD`). |
| **Compliance Milestone** | `DisbursementMilestone` | Tracks whether the **compliance condition** for a schedule entry has been met (`ComplianceStatus`: `PENDING`, `PROOF_SUBMITTED`, `COMPLETED`, `OVERDUE`, `NON_COMPLIANT`) and whether the funds for it have been released (`DisbursementStatus`: `NOT_RELEASED`, `RELEASED`). |

These three concepts are deliberately kept separate:

- **Schedule status** (`ApplicationDisbursementSchedule.status`) — has the money actually been released for this stage?
- **Compliance status** (`DisbursementMilestone.complianceStatus`) — has the beneficiary satisfied the condition (documentation, ground verification, utilization proof) for this stage?
- **Application status** (`Application.status`) — the overall lifecycle position of the application (e.g. `READY_FOR_DISBURSEMENT`, `DISBURSED`, `COMPLETED`).

```mermaid
stateDiagram-v2
    [*] --> PENDING_Schedule
    PENDING_Schedule --> RELEASED_Schedule: Finance releases stage
    RELEASED_Schedule --> [*]

    [*] --> PENDING_Compliance
    PENDING_Compliance --> PROOF_SUBMITTED: Beneficiary uploads proof
    PROOF_SUBMITTED --> COMPLETED_Compliance: Officer verifies
    PENDING_Compliance --> OVERDUE: Due date passes (scheduled job)
    OVERDUE --> NON_COMPLIANT: Manual officer determination
    COMPLETED_Compliance --> [*]
```

A stage's schedule can only be released once the **previous** stage's schedule is `RELEASED` **and** the previous stage's milestone is `COMPLETED`. When every stage's schedule reaches `RELEASED`, the parent `Application` advances to `DISBURSED` (or `COMPLETED` if the final stage's `TriggerMilestone` is `PROJECT_CLOSURE`).

---

## Security & Authorization

- **Spring Security** with a fully stateless (`SessionCreationPolicy.STATELESS`) filter chain; HTTP Basic is disabled.
- **JWT** (`io.jsonwebtoken` / jjwt) issued on successful login or registration, carrying the user's email as subject and role as a claim. `JwtAuthenticationFilter` validates the token on every request and populates the `SecurityContext`.
- **Password hashing** via `BCryptPasswordEncoder`.
- **Role-based endpoint authorization** is centralized in `SecurityConfig` using URL-pattern matchers (`hasRole` / `hasAnyRole`) rather than scattered `@PreAuthorize` annotations — for example, scheme writes are `ADMIN`-only, verification actions are restricted to officer roles, and analytics/reports are restricted to `DISTRICT_OFFICER`, `FINANCE_APPROVER`, and `ADMIN`.
- **Server-resolved identity**: controllers resolve the acting user's ID from the authenticated `Authentication` principal (email → `User` lookup), never from client-supplied request fields. This prevents impersonation (e.g. the old `officerId` field was removed from `VerificationRequestDto` for exactly this reason).
- **Ownership checks**: beneficiaries can only view/act on their own applications, documents, and disbursement schedules (`ApplicationService`, `DocumentService`, `DisbursementController`, `ComplianceMilestoneController`).
- **Strict Regional Isolation**:
  - `FIELD_OFFICER` and `DISTRICT_OFFICER` review queues are strictly partitioned to applications originating within the officer's assigned `region` (e.g. `Maharashtra`). Applications from other states never appear in their queues, preventing cross-jurisdictional leakage.
  - `FINANCE_APPROVER` and `ADMIN` maintain universal (statewide/nationwide) visibility across all applications and budget pots.
- **Self-Contained Auth Response (`AuthResponseDto`)**: `POST /api/v1/auth/login` returns `{ token, type: "Bearer", id, email, fullName, role, region }`. Returning `region` directly guarantees the frontend portal can immediately enforce regional queue filtering and display region-specific context without issuing secondary profile queries.
- **Separation of duties**: `FINANCE_APPROVER` can release disbursement stages but is explicitly blocked from completing compliance milestones.
- **CORS** is configured for known frontend origins (Firebase-hosted portal, Vite dev server on localhost:3000 / localhost:5173).

Request flow:

```text
Login (/api/v1/auth/login)
  → JWT issued (email + role claim) + User metadata (role, region)
  → JWT sent in Authorization: Bearer <token> header on subsequent requests
  → JwtAuthenticationFilter validates token, sets SecurityContext
  → SecurityConfig URL matcher checks role
  → Controller resolves acting user from Authentication principal
  → Service layer enforces ownership / region / business-rule checks
```

---

## Document Management & In-App Viewer

Two distinct categories of documents exist, both backed by the `Document` entity and stored via Cloudinary:

- **KYC documents** (`Document.stage == null`) — uploaded by the beneficiary while an application is `DRAFT` or `RE_VERIFICATION_REQUIRED`, matched against `Scheme.requiredDocuments`. Verified individually by the Field Officer (`DocumentVerificationStatus`: `PENDING`, `VERIFIED`, `REJECTED`) before a Field-level approval is allowed.
- **Stage utilization proofs** (`Document.stage != null`) — uploaded by the beneficiary only after the corresponding disbursement stage has been `RELEASED`, and only once per stage (re-upload is blocked once the milestone is already `COMPLETED`). These are verified through the compliance milestone workflow (`ComplianceMilestoneService`), not the KYC verification endpoint.

### Cloud Storage Backend
All document uploads are persisted to **Cloudinary**, the project's cloud file/media storage service, rather than to local disk. `CloudinaryConfig` (`common/config`) constructs the Cloudinary client from configured credentials, and `CloudinaryService` (`common/service`) wraps the upload call:

- On upload, `DocumentService.uploadDocument()` sends the incoming file's bytes to Cloudinary via `CloudinaryService.upload()`, which stores the asset under the `Subsidy Tracker/documents` Cloudinary folder and returns a secure HTTPS `secure_url`. This URL — not a local file path — is what's persisted on the `Document` entity's `filePath` field.
- On retrieval, `DocumentController.getFile()` checks whether the stored `filePath` is an `http://`/`https://` URL; for Cloudinary-hosted documents it redirects the caller directly to that secure URL (HTTP 302) rather than streaming bytes from local disk.
- Upload failures from Cloudinary are surfaced as an `InvalidOperationException` rather than a raw I/O exception.

This applies uniformly to both KYC documents and stage-linked utilization proofs — both document categories go through the same `CloudinaryService.upload()` path.

### In-App Interactive Document Viewer (`DocumentViewerModal`)
To eliminate the friction of downloading files or navigating away from the workspace, the frontend features an integrated **Document Viewer Modal** across all officer and citizen portals. Users can preview uploaded Aadhaar cards, land records, passbooks, and utilization proofs with interactive zoom, rotation, status badges, and direct verification controls.

Access control (`DocumentService.checkDocumentAccess`):
- **Beneficiary** — only their own application's documents.
- **Field/District Officer** — documents for applications currently at their review stage, in their region; during the disbursement phase they may view stage-linked proofs only (KYC documents are hidden once verification has moved on).
- **Finance Approver** — documents for applications at `FINANCE_REVIEW_PENDING`, statewide.
- **Admin** — full access.

A dedicated download endpoint (`GET /documents/{documentId}/file`) streams the file (redirecting to the Cloudinary URL) after applying the same access rules.

---

## Audit Logging

`AuditLogService` writes an `AuditLog` row (entity name, entity ID, action, actor, timestamp, free-text details) for the following state-changing events:

- Officer registration approval/rejection
- Every verification decision (`APPROVED`, `REJECTED`, `RE_VERIFICATION_REQUESTED`) at Field/District/Finance level
- KYC document verification (`DOCUMENT_VERIFIED` / `DOCUMENT_REJECTED` / `DOCUMENT_PENDING`)
- Application submission
- Disbursement schedule generation
- Disbursement stage release
- Compliance milestone completion
- Compliance milestone becoming overdue (system-generated event, no actor)
- Treasury disbursement dispatch

Audit-log writes are wrapped so that a logging failure never blocks the underlying business operation (failures are logged as warnings, not thrown).

---

## External Integrations

- **Beneficiary Registry Integration** (`/api/v1/integrations/beneficiary/validate`) — calls a pluggable `BeneficiaryRegistryClient`; the current implementation talks to an in-app mock endpoint (`/mock/external-registry/validate`) that returns a hardcoded validation result. This is a standalone validation endpoint and is not currently invoked automatically during beneficiary profile creation.
- **Treasury Integration** (`/api/v1/integrations/treasury/disburse`, `FINANCE_APPROVER`/`ADMIN` only) — calls a pluggable `TreasuryClient`; the current implementation talks to an in-app mock endpoint (`/mock/external-treasury/disburse`) that simulates a treasury transaction ID. This endpoint is implemented and audit-logged, but is a separate, manually-invoked integration point — it is not automatically called as part of the `releaseStage` disbursement flow.

Both mock controllers exist purely to exercise the integration clients in local development/testing without a real external system.

---

## Database / Domain Model

| Entity | Purpose |
|---|---|
| `User` | System account for officers, admins, and beneficiaries; holds email, hashed password, role, and (for regional roles) a region. |
| `Beneficiary` | The person a scheme benefits; linked one-to-one to a `User` account; holds category, region, income, and contact details. |
| `Scheme` | A government subsidy/grant program; holds income limits, allowed categories, required documents, and active flag. |
| `SchemeSlab` | Grant amount for a scheme, per `BeneficiaryCategory`. |
| `RegionalBudget` | Allocated vs. utilized budget for a scheme, per region. |
| `Application` | The event of a beneficiary applying to a scheme; carries `ApplicationStatus`, eligibility score, submission date, and remarks. |
| `Document` | An uploaded file — either a KYC document (`stage == null`) or a stage-linked utilization proof. |
| `Verification` | An immutable record of one officer's decision at one verification level for one application. |
| `DisbursementPlan` | The scheme-level configuration of how many disbursement stages exist. |
| `DisbursementStage` | One configured stage of a plan (percentage, sequence, trigger milestone, due-date offset). |
| `ApplicationDisbursementSchedule` | A per-application instantiation of a stage, with the actual scheduled amount, due date, and release status. |
| `DisbursementMilestone` | Tracks the compliance status and disbursement status for one schedule entry. |
| `OfficerRegistrationRequest` | A pending request from a prospective officer, awaiting Admin approval. |
| `AuditLog` | Immutable audit trail entry for a state-changing action. |

Key relationships: one `User` ↔ one `Beneficiary`; one `Beneficiary` → many `Application`s; one `Scheme` → many `SchemeSlab`s and `RegionalBudget`s; one `Scheme` → one `DisbursementPlan` → many `DisbursementStage`s; one `Application` → many `ApplicationDisbursementSchedule`s and `DisbursementMilestone`s (one pair per stage); one `Application` → many `Document`s and `Verification`s.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend Framework | Spring Boot 3.5.16 (Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring Scheduling) |
| Language / Runtime | Java 17 |
| Frontend Framework | React 18, Vite 5, React Router 6, TanStack Query 5 |
| Frontend Styling & UI | Vanilla CSS + Tailwind CSS tokens, Lucide Icons, Chart.js / React-Chartjs-2 |
| Database (Production / Cloud) | **Neon Serverless PostgreSQL** (PostgreSQL 16) with optimized HikariCP pooling |
| Database (Local / Test) | PostgreSQL / H2 (in-memory) |
| Authentication | JWT (`io.jsonwebtoken` / jjwt 0.11.5) + BCrypt password hashing |
| Build Tool | Maven (with Maven Wrapper, `mvnw` / `mvnw.cmd`) + npm |
| ORM | Hibernate via Spring Data JPA |
| Cloud Document Storage | Cloudinary CDN (direct HTTPS delivery & asset storage) |
| Reporting Engine | Apache POI (`poi-ooxml`, Excel) and OpenPDF (PDF) |
| Deployment | Firebase Hosting (Frontend SPA), Render / Docker (Backend API), Neon (Cloud PostgreSQL) |
| API Style | RESTful JSON, versioned under `/api/v1` for core resources and `/api/disbursement` for the disbursement module |

---

## Project Structure

```text
subsidy-tracker/
├── frontend/                         # Modern React + Vite Single-Page Application
│   ├── src/
│   │   ├── api/                      # Axios HTTP client with auth interceptors
│   │   ├── components/               # Reusable UI components
│   │   │   ├── DocumentViewerModal.jsx  # In-app zoom/rotate/pan document viewer
│   │   │   ├── ApplicationStepper.jsx   # Visual multi-stage progression stepper
│   │   │   ├── MetricCard.jsx           # KPI & analytics metric widgets
│   │   │   ├── Modal.jsx                # Accessible modal container
│   │   │   ├── Navbar.jsx & Sidebar.jsx # Navigation & role-based route chrome
│   │   │   └── charts/                  # Recharts / Chart.js analytic visuals
│   │   ├── context/                  # AuthContext (JWT session, role, and region state)
│   │   ├── pages/                    # Dedicated role-based portals
│   │   │   ├── AdminPortal.jsx          # Scheme, slab, budget & officer governance
│   │   │   ├── BeneficiaryPortal.jsx    # Citizen application submission & status tracking
│   │   │   ├── DistrictOfficerPortal.jsx# Regional stage 2 review & verification
│   │   │   ├── FieldOfficerPortal.jsx   # Regional ground inspection & KYC verification
│   │   │   ├── FinanceApproverPortal.jsx# Tranche release & treasury dispatch
│   │   │   ├── LoginPage.jsx            # Clean credential authentication
│   │   │   └── RegisterPage.jsx         # Citizen self-registration
│   │   ├── App.jsx                   # Router & protected route guards
│   │   └── index.css                 # Platform design system & theme tokens
│   ├── package.json
│   └── vite.config.js
├── src/                              # Spring Boot 3 Backend
│   ├── main/
│   │   ├── java/com/subsidytracker/
│   │   │   ├── analytics/            # Fund utilization & compliance analytics service
│   │   │   ├── beneficiary/          # Beneficiary profile CRUD
│   │   │   ├── common/               # Shared entities, enums, exceptions, security config, audit
│   │   │   │   └── config/           # DatabaseSeeder, CloudinaryConfig, SecurityConfig
│   │   │   ├── dashboard/            # Dashboard DTOs + AnalyticsDataSource abstraction
│   │   │   ├── disbursement/         # Disbursement plans, stages, schedules, compliance milestones
│   │   │   ├── eligibility/          # Applications, eligibility scoring, verification, documents
│   │   │   ├── integration/          # Beneficiary registry & treasury client integrations (+ mocks)
│   │   │   ├── reports/              # Excel/PDF report generation
│   │   │   ├── scheme/               # Scheme, slab, regional budget management
│   │   │   ├── security/             # Auth, JWT, officer registration
│   │   │   ├── user/                 # Lightweight "who am I" / user listing endpoints
│   │   │   └── SubsidyTrackerApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-local.properties.example
├── docs/                             # Design docs, workflow specs, API/analytics documentation
├── Dockerfile
├── render.yaml
├── firebase.json
├── pom.xml
└── README.md
```

---

## Backend Architecture

Each module follows a conventional layered structure:

```text
Controller  (REST endpoints, request/response DTOs, auth-principal resolution)
    ↓
Service     (business rules, transactions, orchestration across repositories)
    ↓
Repository  (Spring Data JPA interfaces)
    ↓
Entity / Database
```

Notable cross-cutting collaborators:
- `AuditLogService` — injected into services that perform state-changing actions.
- `GlobalExceptionHandler` — maps `ResourceNotFoundException` (404), `InvalidOperationException` (400), Spring `AuthenticationException` (401), and any other exception (500) to a consistent JSON error body.
- `AnalyticsDataSource` — an interface the `dashboard`/`reports` packages depend on; `RealAnalyticsDataSourceAdapter` (marked `@Primary`) delegates to the real `AnalyticsService`, keeping reporting decoupled from the analytics implementation.
- `ScheduleGenerationService` and `ComplianceMilestoneService` — the two services that drive the staged-disbursement state machine described above.

---

## API Overview

All endpoints are prefixed `/api/v1` (core resources) except the disbursement module, which uses `/api/disbursement`.

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a beneficiary account |
| POST | `/api/v1/auth/officer-register` | Submit an officer registration request (pending Admin approval) |
| POST | `/api/v1/auth/login` | Authenticate and receive a JWT |

### Users & Officer Administration

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/users/me` | Get the authenticated user's identity/role/region |
| GET | `/api/v1/users` | List all users (Admin) |
| GET | `/api/v1/admin/officer-registration-requests` | List pending officer requests (Admin) |
| GET | `/api/v1/admin/officer-registration-requests/all` | List all officer requests (Admin) |
| POST | `/api/v1/admin/officer-registration-requests/{id}/approve` | Approve an officer request (Admin) |
| POST | `/api/v1/admin/officer-registration-requests/{id}/reject` | Reject an officer request (Admin) |

### Beneficiaries

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/beneficiaries` | Create the caller's beneficiary profile |
| GET | `/api/v1/beneficiaries/me` | Get the caller's own beneficiary profile |
| GET | `/api/v1/beneficiaries/{id}` | Get a beneficiary by ID |
| GET | `/api/v1/beneficiaries` | List/paginate beneficiaries |
| PUT | `/api/v1/beneficiaries/{id}` | Update a beneficiary profile (owner or Admin) |

### Schemes

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/schemes` | Create a scheme (Admin) |
| GET | `/api/v1/schemes/{id}` | Get a scheme (inactive schemes hidden from non-admins) |
| GET | `/api/v1/schemes` | List schemes (active-only for non-admins) |
| PUT | `/api/v1/schemes/{id}` | Update a scheme (Admin) |
| POST | `/api/v1/schemes/{id}/slabs` | Add a grant slab for a category (Admin) |
| GET | `/api/v1/schemes/{id}/slabs` | List slabs for a scheme |
| POST | `/api/v1/schemes/{id}/regional-budgets` | Allocate a regional budget (Admin) |
| GET | `/api/v1/schemes/{id}/regional-budgets` | List regional budgets for a scheme |

### Applications

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/applications` | Create a draft application (Beneficiary) |
| GET | `/api/v1/applications/my-applications` | List the caller's own applications |
| POST | `/api/v1/applications/{id}/submit` | Submit a draft application for eligibility evaluation |
| GET | `/api/v1/applications/{id}` | Get an application (ownership enforced for beneficiaries) |
| GET | `/api/v1/applications` | List/paginate all applications |
| GET | `/api/v1/applications/status/{status}` | List applications by status (officers/Admin) |
| POST | `/api/v1/applications/{applicationId}/calculate-eligibility` | Manually recalculate eligibility (Admin) |
| PATCH | `/api/v1/applications/{applicationId}/verify` | Record a Field/District/Finance verification decision |

### Documents

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/applications/{applicationId}/documents` | Upload a KYC document or (with `stageId`) a utilization proof |
| GET | `/api/v1/applications/{applicationId}/documents` | List documents for an application (role/region scoped) |
| GET | `/api/v1/applications/{applicationId}/documents/{documentId}/file` | Download/view a document's file |
| PATCH | `/api/v1/applications/{applicationId}/documents/{documentId}/verify` | Verify/reject a KYC document (Field Officer) |

### Disbursement

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/disbursement/plans` | Create a disbursement plan for a scheme (Admin) |
| PUT | `/api/disbursement/plans/{planId}` | Replace a plan's stages (Admin) |
| DELETE | `/api/disbursement/plans/{planId}` | Delete a plan (Admin) |
| GET | `/api/disbursement/plans/{planId}` | Get a plan by ID |
| GET | `/api/disbursement/plans/scheme/{schemeId}` | Get a plan by scheme |
| POST | `/api/disbursement/schedules/generate/{applicationId}` | Manually (re-)trigger schedule generation |
| POST | `/api/disbursement/schedules/{scheduleId}/release` | Release a stage's funds (Finance/Admin) |
| GET | `/api/disbursement/schedules/application/{applicationId}` | Get an application's disbursement schedule |

### Compliance

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/disbursement/compliance/application/{applicationId}` | Create compliance milestones from a schedule (Admin) |
| GET | `/api/disbursement/compliance/application/{applicationId}` | List milestones for an application (ownership enforced) |
| PUT | `/api/disbursement/compliance/{milestoneId}/complete` | Mark a milestone compliant (Field/District Officer, Admin) |
| GET | `/api/disbursement/compliance/pending` | List pending milestones (officers/Admin) |
| GET | `/api/disbursement/compliance/overdue` | List overdue milestones (officers/Admin) |

### Analytics & Reports

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/analytics/overview` | Dashboard stat-card rollup |
| GET | `/api/v1/analytics/fund-utilization/schemes` | Fund utilization by scheme |
| GET | `/api/v1/analytics/fund-utilization/regions` | Fund utilization by region |
| GET | `/api/v1/analytics/compliance/pending-milestones` | Pending/overdue/completed milestone counts |
| GET | `/api/v1/analytics/compliance/non-compliance` | Non-compliance counts by scheme/region |
| GET | `/api/v1/analytics/approval-turnaround` | Average/fastest/slowest approval turnaround |
| GET | `/api/v1/analytics/budget-exhaustion-warnings` | Budget exhaustion severity by scheme/region |
| GET | `/api/v1/analytics/beneficiary-category-distribution` | Beneficiary counts by category |
| GET | `/api/v1/reports/schemes/excel` \| `/schemes/pdf` | Scheme-wise utilization report download |
| GET | `/api/v1/reports/regions/excel` \| `/regions/pdf` | Regional utilization report download |

### Integrations

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/integrations/beneficiary/validate` | Validate a beneficiary against the external registry (mocked) |
| POST | `/api/v1/integrations/treasury/disburse` | Dispatch a disbursement order to the treasury system (mocked) |

---

## Application Status Flow

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> NOT_ELIGIBLE: fails income/category
    DRAFT --> MANUAL_REVIEW_REQUIRED: no field officer in region
    DRAFT --> FIELD_VERIFICATION_PENDING: eligibility passed
    FIELD_VERIFICATION_PENDING --> FIELD_REJECTED
    FIELD_VERIFICATION_PENDING --> DISTRICT_REVIEW_PENDING: approved (standard)
    FIELD_VERIFICATION_PENDING --> FINANCE_REVIEW_PENDING: approved (fast-track)
    DISTRICT_REVIEW_PENDING --> FIELD_VERIFICATION_PENDING: re-verification requested
    DISTRICT_REVIEW_PENDING --> DISTRICT_REJECTED
    DISTRICT_REVIEW_PENDING --> FINANCE_REVIEW_PENDING: approved
    FINANCE_REVIEW_PENDING --> DISTRICT_REVIEW_PENDING: re-verification requested
    FINANCE_REVIEW_PENDING --> FINANCE_REJECTED
    FINANCE_REVIEW_PENDING --> READY_FOR_DISBURSEMENT: approved
    READY_FOR_DISBURSEMENT --> DISBURSED: all stages released
    DISBURSED --> COMPLETED: final closure stage released
    NOT_ELIGIBLE --> [*]
    MANUAL_REVIEW_REQUIRED --> [*]
    FIELD_REJECTED --> [*]
    DISTRICT_REJECTED --> [*]
    FINANCE_REJECTED --> [*]
    COMPLETED --> [*]
```

The `ApplicationStatus` enum additionally defines `SUBMITTED`, `ELIGIBILITY_PENDING`, `ELIGIBLE`, `RE_VERIFICATION_REQUIRED`, and `APPLICATION_CANCELLED` values reserved for the model but not currently produced by the implemented service logic described above.

---

## Disbursement & Compliance Statuses

**`DisbursementScheduleStatus`** (has the money moved for this stage?):
- `PENDING` — scheduled, not yet released
- `RELEASED` — funds released to this stage
- `ON_HOLD` — reserved value; not currently set by implemented logic

**`ComplianceStatus`** (has the beneficiary satisfied the stage's condition?):
- `PENDING` — condition not yet met
- `PROOF_SUBMITTED` — beneficiary has uploaded utilization proof, awaiting officer verification
- `COMPLETED` — an officer has verified the proof
- `OVERDUE` — due date passed while still `PENDING` (set automatically by the daily scheduled job)
- `NON_COMPLIANT` — reserved for a manual determination that the requirement will not be met; not currently set automatically

**`DisbursementStatus`** (per-milestone mirror of schedule release):
- `NOT_RELEASED`
- `RELEASED`

**`MilestoneType`** (nature of the compliance requirement): `DOCUMENTATION`, `GROUND_VERIFICATION`, `UTILIZATION_PROOF`

**`TriggerMilestone`** (configured on a `DisbursementStage`): `APPLICATION_APPROVAL`, `GROUND_VERIFICATION`, `UTILIZATION_PROOF`, `PROJECT_CLOSURE`

---

## Configuration

Configuration is split between committed defaults (`application.properties`) and environment variables or git-ignored local override files (`application-local.properties` / `.env`):

```properties
# src/main/resources/application.properties (or application-local.properties)

# ---- Database (Neon Serverless PostgreSQL / Cloud DB) ----
spring.datasource.url=${DB_URL:jdbc:postgresql://<neon-host>/neondb?sslmode=require}
spring.datasource.username=${DB_USERNAME:neondb_owner}
spring.datasource.password=${DB_PASSWORD:<password>}
spring.datasource.driver-class-name=org.postgresql.Driver

# ---- JPA / Hibernate ----
spring.jpa.hibernate.ddl-auto=update
spring.data.web.pageable.serialization-mode=via-dto

# ---- Database Seeding Control ----
app.seeding.enabled=false

# ---- HikariCP Connection Pool (Optimized for Serverless PostgreSQL) ----
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=60000
spring.datasource.hikari.max-lifetime=180000
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.keepalive-time=30000

# ---- JWT Token Config ----
jwt.secret=${JWT_SECRET:<your-256-bit-secret-key>}
jwt.expiration=86400000

# ---- Cloudinary Document Storage ----
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:<cloud-name>}
cloudinary.api-key=${CLOUDINARY_API_KEY:<api-key>}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:<api-secret>}
```

Or via environment variables in a `.env` file (see `.env.example`):

```text
DB_URL=jdbc:postgresql://<neon-host>/neondb?sslmode=require
DB_USERNAME=<your-username>
DB_PASSWORD=<your-password>

JWT_SECRET=<your-jwt-secret-min-256-bits>
JWT_EXPIRATION=86400000

CLOUDINARY_CLOUD_NAME=<your-cloud-name>
CLOUDINARY_API_KEY=<your-api-key>
CLOUDINARY_API_SECRET=<your-api-secret>
```

- `app.seeding.enabled` — Set to `false` in live environments to preserve registered records across reboots. When set to `true`, `DatabaseSeeder` executes with guardrails (`userRepository.count() > 0`) to avoid overwriting modified state.
- `spring.datasource.hikari.*` — Tuned with aggressive keepalive (`30s`) and max-lifetime (`180s`) to prevent connection dropouts across serverless cloud PostgreSQL pools (such as Neon).
- `jwt.secret` / `jwt.expiration` — JWT signing key and token lifetime (24 hours by default).
- `cloudinary.*` — Required for document upload and CDN delivery.
- `treasury.mock.base-url` — Base URL for the treasury integration client.
- Application backend runs on port `8080` by default; the frontend SPA runs on port `3000` (or `5173`).

**Never commit production database credentials, JWT secrets, or Cloudinary keys.** `application-local.properties` and `.env` are listed in `.gitignore`.

---

## Installation & Setup

### 1. Clone the Repository
```bash
git clone <repo-url>
cd subsidy-tracker
```

### 2. Configure Environment & Database
The platform connects to **Neon Serverless PostgreSQL** by default via preconfigured environment parameters, requiring zero local database installation.
To use your own PostgreSQL instance, simply set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in a `.env` file (see `.env.example`).

### 3. Run the Backend API (Spring Boot)
In the project root directory:
```bash
# macOS / Linux
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd spring-boot:run
```
Startup will complete with `Started SubsidyTrackerApplication...`, listening on `http://localhost:8080`.

### 4. Run the Frontend Single-Page App (React + Vite)
In a separate terminal window:
```bash
cd frontend
npm install
npm run dev
```
The React single-page app will launch at `http://localhost:3000` (or `http://localhost:5173`) with API proxying automatically directed to the backend.

### 5. Sign In with Demo Accounts
Open `http://localhost:3000` in your browser and sign in using any of the pre-configured accounts:
- **System Administrator**: `admin@gmail.com` / `123456`
- **Finance Approver**: `fa@gmail.com` / `123456`
- **District Officer (Maharashtra)**: `do@gmail.com` / `123456`
- **Field Officer (Maharashtra)**: `fo@gmail.com` / `123456`
- **Beneficiary Citizen**: `me@gmail.com` / `123456`

Or click **Register** to register a fresh citizen account and submit a new subsidy application.

---

## Design / Business Rules

- Beneficiaries can only create/submit/view applications and documents they own; `beneficiaryId` is always server-resolved from the authenticated account, never client-supplied.
- An application cannot be submitted until every document in `Scheme.requiredDocuments` has been uploaded.
- A Field-level approval cannot be recorded until every KYC document on the application is individually marked `VERIFIED`.
- Field and District Officer actions require the officer's `region` to match the beneficiary's `region`; Finance Approvers and Admins act statewide.
- Fast-track routing (skipping District review) requires eligibility score ≥ 80 **and** the applicable slab's grant amount ≤ 50,000; if no matching slab exists, the application is conservatively **not** fast-tracked.
- Re-verification requests route back to the level below the requester (District doubts Field's work → back to Field; Finance doubts District's work → back to District).
- Only `FINANCE_APPROVER` or `ADMIN` may release a disbursement stage.
- A stage cannot be released until the previous stage's schedule is `RELEASED` **and** its milestone is `COMPLETED`.
- A stage-linked utilization proof can only be uploaded after that stage has been released, and cannot be re-uploaded once the milestone is already `COMPLETED`.
- `FINANCE_APPROVER` is explicitly prohibited from completing compliance milestones.
- Core records (`Beneficiary`, `Scheme`, `Application`) are never hard-deleted, to preserve the audit trail; deactivation (`isActive = false`) is used for schemes instead.
- Region is modeled as a flat string on `User`/`Beneficiary`/`RegionalBudget` — there is no hierarchical Region entity (State → District → Block); this is a deliberate, documented simplification (see `docs/regional-hierarchy.md`).
