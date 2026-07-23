# API Specifications and Integration Plan

## 1. Overview

This document defines the REST API contract for Module 1 (Beneficiary & Scheme Master
Data), and the general conventions all modules should follow so APIs stay consistent
across the team. Module 2/3/4/5 endpoints will be added by their respective owners
following the same conventions.

## 2. General Conventions

- Base path: `/api/v1` (versioned from day one, so future breaking changes don't require
  clients to migrate blindly).
- Request/response bodies use DTOs, never entities directly (see reasoning in chat —
  avoids leaking internal fields and avoids LAZY-loading serialization errors).
- Standard error response shape, used by every endpoint:

```json
{
  "timestamp": "2026-07-21T10:15:30",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Beneficiary with id 42 not found"
}
```

- `DELETE` is intentionally not exposed for core records (`Beneficiary`, `Scheme`,
  `Application`) — this is a government compliance/audit system, so hard deletion would
  destroy the audit trail. Deactivation (`isActive = false`) is used instead, where
  applicable.
- Pagination on all list (`GET` collection) endpoints via `?page=0&size=20` query params
  — deferred detail, but reserved now so URLs don't need to change later.

## 3. Beneficiary Endpoints

| Method | Path | Purpose | Request Body | Response |
|---|---|---|---|---|
| POST | `/api/v1/beneficiaries` | Register a new beneficiary | `BeneficiaryRequestDto` (fullName, nationalIdNumber, phoneNumber, address, category) | `BeneficiaryResponseDto` (includes generated `id`, `registrationDate`) |
| GET | `/api/v1/beneficiaries/{id}` | Fetch one beneficiary | — | `BeneficiaryResponseDto` |
| GET | `/api/v1/beneficiaries` | List/search beneficiaries (filterable by category, region) | — (query params: `category`, `region`, `page`, `size`) | Paginated list of `BeneficiaryResponseDto` |
| PUT | `/api/v1/beneficiaries/{id}` | Update beneficiary details | `BeneficiaryRequestDto` | `BeneficiaryResponseDto` |

## 4. Scheme Endpoints

| Method | Path | Purpose | Request Body | Response |
|---|---|---|---|---|
| POST | `/api/v1/schemes` | Create a new scheme | `SchemeRequestDto` (name, description, minIncome, maxIncome, allowedCategories, isActive) | `SchemeResponseDto` |
| GET | `/api/v1/schemes/{id}` | Fetch one scheme | — | `SchemeResponseDto` |
| GET | `/api/v1/schemes` | List schemes (filterable by isActive) | — | Paginated list of `SchemeResponseDto` |
| PUT | `/api/v1/schemes/{id}` | Update scheme details | `SchemeRequestDto` | `SchemeResponseDto` |

### 4.1 Scheme Slabs (nested — a slab only exists in relation to a scheme)

| Method | Path | Purpose | Request Body | Response |
|---|---|---|---|---|
| POST | `/api/v1/schemes/{schemeId}/slabs` | Add a grant-amount slab for a category | `SchemeSlabRequestDto` (category, grantAmount) | `SchemeSlabResponseDto` |
| GET | `/api/v1/schemes/{schemeId}/slabs` | List all slabs for a scheme | — | List of `SchemeSlabResponseDto` |

### 4.2 Regional Budgets (nested — same reasoning as slabs)

| Method | Path | Purpose | Request Body | Response |
|---|---|---|---|---|
| POST | `/api/v1/schemes/{schemeId}/regional-budgets` | Allocate a budget for a region under this scheme | `RegionalBudgetRequestDto` (regionName, allocatedBudget) | `RegionalBudgetResponseDto` |
| GET | `/api/v1/schemes/{schemeId}/regional-budgets` | List all regional budgets for a scheme | — | List of `RegionalBudgetResponseDto` |

## 5. Application Endpoints (Module 2 — included here for API-shape consistency, owned by Module 2)

| Method | Path | Purpose | Request Body | Response |
|---|---|---|---|---|
| POST | `/api/v1/applications` | Submit a new application (staff-initiated, see design-decisions.md) | `ApplicationRequestDto` (beneficiaryId, schemeId) | `ApplicationResponseDto` |
| GET | `/api/v1/applications/{id}` | Fetch one application, including current status | — | `ApplicationResponseDto` |
| PATCH | `/api/v1/applications/{id}/status` | Advance/update application status (used by verification workflow) | `{ "newStatus": "FIELD_APPROVED", "remarks": "..." }` | `ApplicationResponseDto` |

Note: `PATCH` (not `PUT`) is used here specifically because this endpoint changes *one*
field (status) with workflow-specific rules (valid transitions only), not a full
replace-the-resource update — `PUT` semantically implies replacing the whole object.

## 6. External Integration Plan (Treasury / Beneficiary Database Systems)

Per the brief's outcome "REST API integration support for treasury and beneficiary
database systems" — this is Module 5 scope, deployed in Weeks 7–8. Deferred for now;
noted here only so it isn't forgotten. No integration-specific endpoints are designed yet,
since Module 5 hasn't started.

## 7. Authentication Note

All endpoints above will require authentication once Spring Security is fully implemented
(Week 7–8, per existing plan). Until then, endpoints are open/unsecured in local
development — a known, temporary state, not a final design.