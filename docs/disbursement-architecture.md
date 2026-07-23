# Disbursement Process Architecture

## 1. Overview

This doc defines how staged fund release works once an application reaches
`READY_FOR_DISBURSEMENT`. Per the brief, disbursement is not a single lump-sum payment —
it is split into stages ("milestones"), each tied to a compliance condition
(documentation, ground verification, utilization proof), released only once that
condition is met.

This is a new design — no existing entity currently models this. `ApplicationStatus`
tracks the *application's* overall lifecycle, but has no concept of partial,
milestone-by-milestone payment. That gap is what this doc resolves.

## 2. New Entity: `DisbursementMilestone`

| Field | Type | Purpose |
|---|---|---|
| `id` | Long (PK) | |
| `application` | `Application` (`@ManyToOne`, LAZY) | which application this milestone belongs to |
| `milestoneType` | enum: `DOCUMENTATION`, `GROUND_VERIFICATION`, `UTILIZATION_PROOF` | which compliance stage this is |
| `sequenceOrder` | int | enforces order — stage 1 must complete before stage 2, etc. |
| `scheduledAmount` | BigDecimal | how much this specific stage releases |
| `dueDate` | LocalDate | deadline for this stage's condition to be met |
| `complianceStatus` | enum: `PENDING`, `COMPLETED`, `OVERDUE`, `NON_COMPLIANT` | has the *condition* been satisfied |
| `disbursementStatus` | enum: `NOT_RELEASED`, `RELEASED` | has the *money* actually moved |
| `actualDisbursedDate` | LocalDate (nullable) | when the money was actually paid |

One `Application` has many `DisbursementMilestone` rows — a one-to-many relationship, same
pattern as `Scheme` → `SchemeSlab`.

## 3. Key Design Decisions

**3.1 — `milestoneType` is a fixed enum, not a configurable field.**
The brief states the three-stage structure (documentation, ground verification,
utilization proof) as the model to follow, and it mirrors the existing verification
sequence (Field → District → Finance). A fixed enum keeps this consistent with
`ApplicationStatus` and is simpler to implement correctly within the project timeline.
*Documented simplification*: if a real system needed different milestone sets per scheme,
this would need to become a configurable list per `Scheme` instead of a shared enum —
deferred as a future improvement, same pattern as `allowedCategories`.

**3.2 — `complianceStatus` and `disbursementStatus` are two separate fields, not one.**
These represent two different real-world moments: a milestone can be verified/compliant
today, while the actual fund transfer happens days later. Collapsing these into a single
status would make it impossible to answer a required report from Module 4 — "pending
milestone summaries" (i.e., milestones that are compliant but not yet paid out). Keeping
them separate lets that query be a simple filter: `complianceStatus = COMPLETED AND
disbursementStatus = NOT_RELEASED`.

**3.3 — `scheduledAmount` per milestone must sum to the beneficiary's grant amount.**
The total of all `DisbursementMilestone.scheduledAmount` rows for one application should
equal the `grantAmount` from the matching `SchemeSlab` (looked up by the beneficiary's
category). This is a validation rule enforced in the service layer when milestones are
created — not a database constraint, since summing across rows can't be expressed as a
simple column constraint in MySQL without a trigger, which is out of scope here.

**3.4 — Milestones are created once, when the application reaches
`READY_FOR_DISBURSEMENT`.**
The three `DisbursementMilestone` rows for an application are generated at that point
(sequence 1/2/3, with `scheduledAmount` split according to the scheme's configuration),
not before. This avoids creating disbursement records for applications that never reach
that stage (rejected, not eligible, etc.).

## 4. How This Connects to `ApplicationStatus`

`Application.status` moves to `DISBURSED` only once **all** `DisbursementMilestone` rows
for that application have `disbursementStatus = RELEASED`. `COMPLETED` (the final status)
is set once all milestones are released and any final compliance check (e.g. final
utilization proof) is confirmed. No changes are needed to the `ApplicationStatus` enum
itself — this is purely a service-layer rule that reads milestone data to decide when to
advance the existing status.

## 5. Non-Compliance and Reminders

Per the brief's requirement for "compliance reminder automation, and non-compliance
flagging":
- A milestone's `complianceStatus` moves from `PENDING` to `OVERDUE` automatically once
  `dueDate` passes without completion — this is a scheduled check, not a manual action.
- `NON_COMPLIANT` is a manual/reviewed status (e.g. an officer determines the beneficiary
  will not meet the requirement at all, not just late) — distinct from `OVERDUE`, which
  just means the deadline passed but resolution is still possible.
- Reminder automation (notifying staff before `dueDate`) is an implementation detail for
  Milestone 3, not a schema concern — no new field needed for it beyond `dueDate` itself.

## 6. Impact on Existing Entities

| Entity | Change |
|---|---|
| `Application` | No structural change — gains a one-to-many relationship to the new `DisbursementMilestone` |
| `SchemeSlab` | No change — remains the source of truth for total grant amount per category |
| `ApplicationStatus` | No change — advancement rule (Section 4) lives in service logic, not the enum |

## 7. Future Improvements (Deferred, Documented)

- Configurable milestone sets per scheme (Section 3.1), if different schemes need
  different stage structures.
- Enforcing the amount-sum validation (Section 3.3) at the database level via a trigger or
  check, instead of only in the service layer — deferred as unnecessary complexity for
  project scope.