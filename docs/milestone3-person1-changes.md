# Milestone 3 - Person 1 Implementation Changes

## Overview
Implementation of Staged Disbursement Plan & Schedule Engine.

## Changes

### 1. Project Setup
- Created disbursement module package structure (`controller`, `dto`, `entity`, `repository`, `service`, `exception`, `mapper`).

### 2. Enums
- Added `DisbursementScheduleStatus` enum (`PENDING`, `RELEASED`, `ON_HOLD`).
- Added `TriggerMilestone` enum (`APPLICATION_APPROVAL`, `GROUND_VERIFICATION`, `UTILIZATION_PROOF`, `PROJECT_CLOSURE`).

### 3. Entities
- Implemented `DisbursementPlan` entity (Scheme-level reusable disbursement plan with `scheme`, `numberOfStages`, `createdBy`, `createdAt`).
- Created `DisbursementStage` entity:
  - Added relationship with `DisbursementPlan` (`@ManyToOne` with foreign key `plan_id`).
  - Added stage configuration fields (`stageName`, `sequenceNumber`, `percentageOfGrant`, `triggerMilestone`).
- Created `ApplicationDisbursementSchedule` entity:
  - Linked schedule to `Application` (`@ManyToOne` with foreign key `application_id`).
  - Linked schedule to `DisbursementStage` (`@ManyToOne` with foreign key `stage_id`).
  - Added scheduled amount (`scheduledAmount`), due date (`dueDate`), and status (`DisbursementScheduleStatus`) fields.

### 4. Repositories
- Created `DisbursementPlanRepository`, `DisbursementStageRepository`, and `ApplicationDisbursementScheduleRepository`.
- Added plan lookup by scheme (`findBySchemeId`).
- Added ordered stage retrieval (`findByPlanIdOrderBySequenceNumberAsc`).
- Added schedule existence check (`existsByApplicationId`), ordered schedule retrieval (`findByApplicationIdOrderByStageSequenceNumberAsc`), and application lookup (`findByApplicationId`).

### 5. Services
- Added `DisbursementPlanService` with CRUD operations:
  - `createPlan` — creates plan with stages for a scheme.
  - `updatePlan` — replaces stage configuration on an existing plan.
  - `deletePlan` — removes plan and all associated stages.
  - `getPlanBySchemeId` — retrieves plan by scheme.
  - `getPlanById` — retrieves plan by id.
- Added validation rules:
  - Stage percentages must total exactly 100%.
  - Percentage of grant cannot be null, must be > 0, and must be <= 100.
  - Sequence numbers must be unique, cannot be null, and must be > 0.
  - Trigger milestone is required for each stage.
  - Stage names must be unique.
  - `numberOfStages` is derived from the configured stages count.
  - `createdByUserId` is mandatory when creating a plan.
- Write operations marked `@Transactional`.
- Uses `saveAll()` for batch stage persistence.
- Uses existing project exceptions (`ResourceNotFoundException`, `InvalidOperationException`).

### 6. Business Rules
- One disbursement plan per scheme.
- One schedule per application.
- Stage percentages must total exactly 100%.
- Stage sequence numbers must be unique.
- Stage names must be unique.
- Stages are always processed in ascending sequence number.

### 7. Schedule Generation
- Implemented `ScheduleGenerationService`.
- Generates milestone schedule from configured plan.
- Prevents duplicate schedule generation.
- Uses ordered disbursement stages.
- `scheduledAmount` is calculated using the applicable `SchemeSlab.grantAmount` resolved by scheme + beneficiary category.
- Amount per stage: `SchemeSlab.grantAmount × stage.percentageOfGrant / 100` (BigDecimal, 2 decimal places, HALF_UP rounding).
- Missing SchemeSlab for the application's scheme + beneficiary category throws `InvalidOperationException` (no silent fallback to zero or `Scheme.grantAmount`).
- `dueDate` is calculated automatically per stage: `generationDate + stage.dueDateOffsetDays`.
- A single `generationDate` (the date schedule generation runs) is used as the base for all stages in a schedule.
- If a stage has no configured `dueDateOffsetDays` (null), schedule generation fails with `InvalidOperationException` — no silent fallback to 0 or today's date.
- Actual offset values are configuration data set per-stage in the disbursement plan, not hardcoded business rules.

### 8. DTOs
- Created `DisbursementPlanRequest` (schemeId + stages list).
- Created `DisbursementStageRequest` (stageName, sequenceNumber, percentageOfGrant, triggerMilestone, dueDateOffsetDays).
- Created `DisbursementPlanResponse` (id, schemeId, numberOfStages, createdById, createdAt, stages list).
- Created `DisbursementStageResponse` (id, planId, stageName, sequenceNumber, percentageOfGrant, triggerMilestone, dueDateOffsetDays).
- Created `ScheduleEntryResponse` (id, applicationId, stageId, stageName, stageSequenceNumber, scheduledAmount, dueDate, status).

### 9. Controllers
- Added `DisbursementController` at `/api/disbursement`.
- Injects `DisbursementPlanService` and `ScheduleGenerationService` only.
- Exposed plan management endpoints:
  - `POST   /api/disbursement/plans` — create disbursement plan (authenticated).
  - `PUT    /api/disbursement/plans/{planId}` — update plan stages.
  - `DELETE /api/disbursement/plans/{planId}` — delete plan.
  - `GET    /api/disbursement/plans/{planId}` — get plan by id.
  - `GET    /api/disbursement/plans/scheme/{schemeId}` — get plan by scheme.
- Exposed schedule endpoints:
  - `POST   /api/disbursement/schedules/generate/{applicationId}` — generate schedule.
  - `GET    /api/disbursement/schedules/application/{applicationId}` — get schedule by application.
- Added `getScheduleByApplication` to `ScheduleGenerationService` so the controller delegates entirely to services.
- Controller is thin: all business logic remains in services.

### 9. Testing

Implemented unit tests for:

- DisbursementPlanServiceTest
  - 18 test cases covering creation flow, validation rules (including dueDateOffsetDays null/negative), duplicate prevention, and exception handling.

- ScheduleGenerationServiceTest
  - 9 test cases covering schedule generation, validation failures, duplicate prevention, SchemeSlab resolution, and retrieval.

Testing performed using JUnit and Mockito.
All tests verified using Maven test lifecycle.


### 11. Integration Notes
- Schedule generation is implemented in `ScheduleGenerationService`.
- Integrated automatic schedule generation into `VerificationService.processVerification(...)`: when Finance approval advances an application status to `READY_FOR_DISBURSEMENT`, `ScheduleGenerationService.generateSchedule(applicationId)` is automatically invoked within the same transaction.
- The manual schedule-generation endpoint `POST /api/disbursement/schedules/generate/{applicationId}` remains available.

### 12. Future TODOs

- Integrate schedule generation with the application approval workflow.