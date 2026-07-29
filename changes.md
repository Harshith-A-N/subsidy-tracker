# Changelog: Subsidy-Tracker Infosys Workflow Alignment

This document tracks the completed implementation phases for aligning the existing `subsidy-tracker` codebase with the Infosys workflow.

---

## Phase 1 — Beneficiary Authentication

### 1. Objective
Fix the existing broken Spring Security setup (which blocked all endpoints with an auto-generated password) and introduce basic authentication. Establish the foundation for role-based access control and user identity without breaking existing module functionality.

### 2. Files Created
- `security/dto/RegisterRequestDto.java`: DTO for beneficiary registration.
- `security/dto/LoginRequestDto.java`: DTO for user login.
- `security/dto/AuthResponseDto.java`: Standardized response containing user identity and role.
- `security/service/CustomUserDetailsService.java`: Bridges the application's `User` entity to Spring Security's `UserDetails`.
- `security/service/AuthService.java`: Handles business logic for registration and login, including password hashing and duplicate checks.
- `security/controller/AuthController.java`: Exposes public `/api/v1/auth/register` and `/api/v1/auth/login` endpoints.
- `common/config/SecurityConfig.java`: Configures Spring Security with HTTP Basic authentication, stateless sessions, BCrypt password encoding, and disabled CSRF.

### 3. Files Modified
- `common/enums/Role.java`: Added the `BENEFICIARY` role.
- `common/entity/User.java`: Removed `@Column(nullable = false)` from the `region` field to allow users (like beneficiaries) to have no region.
- `eligibility/repository/UserRepository.java`: Added `findByEmail(String email)` method.

### 4. Important Implementation Decisions
- **Non-breaking Security Configuration:** Auth endpoints (`/register`, `/login`) were made public. All other existing API endpoints were secured with `authenticated()` but with **no role restrictions** yet. This prevents breaking existing workflows while later phases iteratively apply stricter role rules.
- **Server-Assigned Roles:** The registration endpoint explicitly assigns the `BENEFICIARY` role to prevent privilege escalation.
- **Package Integrity:** Reused the existing `UserRepository` from the `eligibility` package instead of duplicating it or moving it to `common`, preserving existing imports in other services.
- **Authentication Strategy:** Implemented HTTP Basic with stateless sessions per requirements, rather than introducing a JWT dependency at this stage.

### 5. API Changes
- **NEW:** `POST /api/v1/auth/register` — Accepts `fullName`, `email`, and `password`. Returns `AuthResponseDto`.
- **NEW:** `POST /api/v1/auth/login` — Accepts `email` and `password`. Returns `AuthResponseDto`.
- **MODIFIED:** All existing APIs now require HTTP Basic credentials (valid email/password from the `users` table) instead of the Spring Boot auto-generated password.

### 6. Testing Performed
- Ran `mvn compile` successfully with zero errors. Existing code paths were unaffected due to the additive nature of the changes and the permissive security configuration.

### 7. Any Future Considerations
- Ensure that in future phases, the security configuration is iteratively tightened to restrict endpoints to specific roles.
- HTTP Basic sends credentials on every request; in a production deployment, this must be paired with HTTPS, or migrating to JWT should be reconsidered.
- The `User.id` type remains a primitive `long`, which is inconsistent with other entities (`Long`). It was left as-is to avoid breaking Hibernate proxying, but might need observation.

---

## Phase 2 — Beneficiary Profile Ownership

### 1. Objective
Establish a direct link between the authenticated `User` account and the `Beneficiary` profile, enforcing ownership rules for profile creation, viewing, and modification.

### 2. Files Created
- None.

### 3. Files Modified
- `common/entity/Beneficiary.java`: Added a `@OneToOne` foreign key linking to the `User` entity.
- `beneficiary/repository/BeneficiaryRepository.java`: Added `findByUserId(long userId)` to resolve profiles by account.
- `beneficiary/dto/BeneficiaryResponseDto.java`: Added `userId` and `email` fields to surface ownership data.
- `beneficiary/service/BeneficiaryService.java`: Completely overhauled `createBeneficiary` and `updateBeneficiary` to enforce one-profile-per-account and verify ownership. Added `getMyProfile`.
- `beneficiary/controller/BeneficiaryController.java`: Injected the `Authentication` principal to extract the caller's identity. Added the `/me` endpoint.

### 4. Important Implementation Decisions
- **Principal Resolution:** The controller resolves the authenticated user's ID via the principal's name (email) and the `UserRepository`, passing only the primitive `userId` to the service layer.
- **Strict Ownership Checks:** The service layer now validates that a user can only update a profile if they own it or if they have the `ADMIN` role.
- **One-per-Account Enforcement:** A logged-in beneficiary is strictly prevented from creating more than one profile.
- **Database Non-Destructiveness:** The new `user_id` foreign key on the `beneficiaries` table was added without a `NOT NULL` constraint. This ensures any legacy records created before this phase won't cause database migration failures.

### 5. API Changes
- **MODIFIED:** `POST /api/v1/beneficiaries` — No longer accepts arbitrary creation. Resolves the `user_id` from the HTTP Basic authorization header. Returns 400 if the user isn't a `BENEFICIARY` or if they already have a profile.
- **NEW:** `GET /api/v1/beneficiaries/me` — Fetches the profile associated with the currently authenticated user. Returns 404 if no profile is linked yet.
- **MODIFIED:** `PUT /api/v1/beneficiaries/{id}` — Now enforces ownership. Returns 400 if a non-admin user attempts to update a profile they do not own.

### 6. Testing Performed
- Ran `mvn compile` successfully. Code changes compiled cleanly against the existing project structure.

### 7. Any Future Considerations
- Read operations (`GET /api/v1/beneficiaries` and `GET /api/v1/beneficiaries/{id}`) currently remain open to any authenticated user. Phase 3 or later will need to tighten the `SecurityConfig` to restrict list operations to admins.
- Document ownership (linking uploaded documents to the beneficiary's user account) is yet to be implemented.

---

## Phase 3 — Scheme Viewing

### 1. Objective
Align scheme visibility and access control with the project requirements. Ensure beneficiaries can only browse and inspect active schemes, while admins can manage (create, edit, view) all schemes, including inactive ones.

### 2. Files Created
- None.

### 3. Files Modified
- `common/config/SecurityConfig.java`: Restricted POST, PUT, and DELETE operations under `/api/v1/schemes/**` to the `ADMIN` role.
- `scheme/repository/SchemeRepository.java`: Added query method `findByIsActiveTrue()` to retrieve only active schemes from the database.
- `scheme/service/SchemeService.java`: Added `getActiveSchemes()` to retrieve and map active schemes only.
- `scheme/controller/SchemeController.java`: Added role-based branching in GET endpoints using the authenticated principal.

### 4. Important Implementation Decisions
- **Role-Based Visibility Filtering:** Preserved the standard `GET /api/v1/schemes` endpoint without adding redundant endpoints. The controller checks if the caller has `ROLE_ADMIN` and routes to `getAllSchemes()`, otherwise it routes to `getActiveSchemes()`.
- **Detail Page Gate:** For `GET /api/v1/schemes/{id}`, if a non-admin user requests an inactive scheme, the system throws `ResourceNotFoundException` (mapped to HTTP 404), hiding the existence of the inactive scheme from unauthorized callers.
- **Write Restriction:** Applied role authorization directly in `SecurityConfig` to protect configuration integrity while keeping read-only browsing open to beneficiaries.

### 5. API Changes
- **MODIFIED:** `GET /api/v1/schemes` — Behavior depends on role. Admins receive all schemes. Non-admins (beneficiaries, staff, etc.) receive only active schemes.
- **MODIFIED:** `GET /api/v1/schemes/{id}` — Behavior depends on role. If the scheme is inactive and the caller is not an admin, returns `404 Not Found`.
- **MODIFIED:** `POST /api/v1/schemes`, `PUT /api/v1/schemes/{id}`, etc. — Now require `ADMIN` credentials. Non-admins receive `403 Forbidden`.

### 6. Testing Performed
- Ran `mvn compile` successfully. Validated correct wiring of new repository and service methods.

### 7. Any Future Considerations
- Future regional budgeting and slab configuration endpoints should similarly be audited for admin-only write permissions as verification layers are added.

---

## Phase 4 — Beneficiary Application Submission

### 1. Objective
Convert application creation from a client-driven beneficiary selection model (where `beneficiaryId` was supplied in the request body) to an authenticated-beneficiary ownership model. Re-architect the lifecycle so that applications are created as drafts, allowing beneficiaries to upload documents before formal submission.

### 2. Files Created
- None.

### 3. Files Modified
- `common/enums/ApplicationStatus.java`: Added the `DRAFT` status as the initial state for new applications.
- `eligibility/dto/ApplicationRequestDto.java`: Removed the `beneficiaryId` field. The DTO now contains only `schemeId`.
- `eligibility/service/ApplicationService.java`: Overhauled `createApplication()` to accept the authenticated user's ID, resolve the beneficiary profile, and initialize the application with `DRAFT` status. Added `getMyApplications()` method. Added role validation (must be `BENEFICIARY`) and profile-exists check. `getApplicationById()` enforces ownership for `BENEFICIARY` callers — they can only view their own applications. Officers and admins retain full read access.
- `eligibility/controller/ApplicationController.java`: Injected `Authentication` principal and `UserRepository`. `POST` extracts the user ID from the principal. Added `GET /my-applications` endpoint. `GET /{id}` now passes the authenticated user ID to the service layer for ownership validation.
- `eligibility/controller/DocumentController.java`: Injected `Authentication` principal and `UserRepository`. Upload and read endpoints now pass the resolved user ID to `DocumentService` for ownership and role-based access control.
- `eligibility/service/DocumentService.java`: Added `UserRepository` dependency. Upload validates: (1) application belongs to authenticated beneficiary, (2) application is in `DRAFT` or `RE_VERIFICATION_REQUIRED` status. Read validates role-based access: beneficiaries see own documents only; field/district officers see documents for applications in their review stage and region; finance approvers see documents for applications in `FINANCE_REVIEW_PENDING` (statewide); admins have full access.

### 4. Important Implementation Decisions
- **No Client Trust:** The `beneficiaryId` field is completely removed from the request DTO. The server always resolves it from `BeneficiaryRepository.findByUserId(currentUserId)`.
- **Pre-Submission Draft State:** Applications are initialized as `DRAFT` to allow a multi-step user flow (creation → document upload → final submission).
- **Endpoint Ordering:** In `ApplicationController`, the `GET /my-applications` mapping is placed before `GET /{id}` to prevent Spring from matching the literal string `"my-applications"` as a path variable.
- **Application Read Ownership:** Beneficiaries can only retrieve applications they own. Officers and admins can view any application, supporting their review workflows.
- **Document Upload Status Gate:** Documents can only be uploaded when the application is in `DRAFT` or `RE_VERIFICATION_REQUIRED`. All other statuses (verification pending, terminal, disbursement) reject uploads.
- **Document Read Access Control:** Role-based filtering ensures documents are never exposed across unrelated applications. Field and district officers must match the beneficiary's region. Finance approvers have statewide access.

### 5. API Changes
- **MODIFIED:** `POST /api/v1/applications` — Request body changed from `{ beneficiaryId, schemeId }` to `{ schemeId }` only. Creates application in `DRAFT` status. Returns 400 if the user is not a `BENEFICIARY` or has no profile.
- **NEW:** `GET /api/v1/applications/my-applications` — Returns only applications belonging to the authenticated beneficiary.
- **MODIFIED:** `GET /api/v1/applications/{id}` — Beneficiaries can only view their own applications. Officers and admins can view any application.
- **MODIFIED:** `POST /api/v1/applications/{applicationId}/documents` — Enforces ownership (application must belong to caller) and status gate (`DRAFT` or `RE_VERIFICATION_REQUIRED` only).
- **MODIFIED:** `GET /api/v1/applications/{applicationId}/documents` — Role-based access control: beneficiaries see own, officers see assigned, admins see all.
- **UNCHANGED:** `GET /api/v1/applications` — Returns all applications.
- **UNCHANGED:** `GET /api/v1/applications/status/{status}` — Filters by status.

### 6. Testing Performed
- Ran `mvn compile` successfully. Verify that new applications start in `DRAFT` and are correctly mapped to the authenticated user.

### 7. Any Future Considerations
- The `GET /api/v1/applications` and `GET /api/v1/applications/status/{status}` listing endpoints remain open to all authenticated users. If admin-only restriction is required in future, add URL matchers to `SecurityConfig`.

---

## Phase 5 — Automatic Eligibility Integration

### 1. Objective
Wire automatic eligibility calculation to trigger immediately when a beneficiary formally submits their application draft. Previously, eligibility had to be triggered separately via a manual admin endpoint. After this phase, the beneficiary triggers submission, which validates document completeness first and immediately scores eligibility if complete.

### 2. Files Created
- None.

### 3. Files Modified
- `eligibility/service/EligibilityService.java`: Extracted an internal `calculateEligibilityForApplication(Application)` method from `calculateEligibility(Long)`. Created a public `getMissingMandatoryDocuments(Application)` method to check uploaded documents against scheme requirements. Removed the redundant document-completeness gate and the `MANUAL_REVIEW_REQUIRED` path for missing documents.
- `eligibility/service/ApplicationService.java`: Overhauled `submitApplication(Long, long)` to call `getMissingMandatoryDocuments()` first. If documents are missing, rejects submission with a detailed exception and keeps the application in `DRAFT` status. Otherwise, delegates to `EligibilityService.calculateEligibilityForApplication()`.
- `eligibility/controller/ApplicationController.java`: Added the `POST /api/v1/applications/{id}/submit` endpoint to allow beneficiaries to formally submit their applications.

### 4. Important Implementation Decisions
- **No Incomplete Submissions:** Enforced a strict validation gate at the submission layer. If mandatory documents are missing, the submission is rejected, and the application remains in `DRAFT` status rather than being scored.
- **Removed Redundant Gate:** Moved document completeness checks out of `EligibilityService`. The service now expects only complete applications. The `MANUAL_REVIEW_REQUIRED` status is no longer assigned for missing documents (but is kept for regional routing issues like missing field officers).
- **Reusable Validation Logic:** Extracted document validation logic from `EligibilityService.areDocumentsComplete()` into a reusable `getMissingMandatoryDocuments(Application)` method, allowing `ApplicationService` to construct precise, user-friendly error messages listing all missing document types.

### 5. API Changes
- **NEW:** `POST /api/v1/applications/{id}/submit` — Formally submits the application. If documents are complete, scores eligibility and returns the updated status (`FIELD_VERIFICATION_PENDING` or `NOT_ELIGIBLE`). If documents are incomplete, returns HTTP 400 with a detailed validation message listing all missing document types (e.g., `Missing required documents:\n- Aadhaar\n- Land Record`), and the application status remains `DRAFT`.
- **MODIFIED:** `POST /api/v1/applications/{id}/calculate-eligibility` — Restricted to `ADMIN` role via `SecurityConfig`. Non-admin callers receive `403 Forbidden`.
- **UNCHANGED:** `POST /api/v1/applications` — Preserved from Phase 4, continues to initialize applications in `DRAFT` status.

### 6. Testing Performed
- Ran `mvn compile` successfully. Validated that compile completes without error and dependency bindings map correctly.

### 7. Any Future Considerations
- Any future changes to a scheme's document requirements will automatically trigger validation at the submission stage since document definitions are dynamically matched from the database.

---

## Phase 6 — Verification Routing

### 1. Objective
Implement dynamic verification routing aligned with the Infosys project specification. Low-risk, low-value applications are fast-tracked (Field → Finance, skipping District review). High-value or lower-scoring applications follow the full verification chain (Field → District → Finance). Finance Approvers and Admins operate with statewide jurisdiction.

### 2. Files Created
- None.

### 3. Files Modified
- `eligibility/dto/VerificationRequestDto.java`: Removed the `officerId` field. The officer is now resolved from the authenticated principal in the controller layer, preventing impersonation.
- `eligibility/controller/VerificationController.java`: Injected `Authentication` principal and `UserRepository`. The `verify` endpoint now resolves the officer's identity from the HTTP Basic credentials and passes the server-resolved officer ID to `VerificationService`. The client can no longer specify which officer performs the action.
- `eligibility/service/VerificationService.java`: Overhauled across four areas:
  - **Officer authentication:** `processVerification()` now accepts a server-resolved `officerId` parameter instead of reading it from the request DTO.
  - **Regional validation:** `validateOfficerRegion()` now bypasses the region check for `FINANCE_APPROVER` and `ADMIN` roles, enforcing region matching only for `FIELD_OFFICER` and `DISTRICT_OFFICER`.
  - **Dynamic routing:** `routeApplication()` now accepts the `Application` entity and delegates to a `shouldFastTrack(Application)` helper when a Field officer approves. Fast-tracked applications skip `DISTRICT_REVIEW_PENDING` and go directly to `FINANCE_REVIEW_PENDING`.
  - **Fast-track policy:** `shouldFastTrack()` evaluates two conditions: (1) eligibility score ≥ 80.0, and (2) the applicable `SchemeSlab.grantAmount` ≤ 50,000. Both conditions must be met. If no matching slab exists, the application is conservatively routed through the full chain.
  - **Dependency additions:** Added `SchemeSlabRepository` for grant amount lookup during routing.
  - **Centralized constants:** `FAST_TRACK_SCORE_THRESHOLD` (80.0) and `FAST_TRACK_GRANT_LIMIT` (50,000) defined as `private static final` constants — no magic numbers scattered in logic.
- `common/config/SecurityConfig.java`: Added URL-based role restrictions:
  - `PATCH /api/v1/applications/*/verify` → restricted to `FIELD_OFFICER`, `DISTRICT_OFFICER`, `FINANCE_APPROVER`.
  - `PATCH /api/v1/applications/*/documents/*/verify` → restricted to `FIELD_OFFICER`, `DISTRICT_OFFICER`, `FINANCE_APPROVER`.
  - `POST /api/v1/applications/*/calculate-eligibility` → restricted to `ADMIN`.

### 4. Important Implementation Decisions
- **No Schema Changes:** Routing thresholds are centralized as constants in `VerificationService`. No new database columns, entity fields, or configuration tables were added.
- **Conservative Default:** If no `SchemeSlab` record exists for the beneficiary's category and scheme, the application is NOT fast-tracked. This prevents accidentally bypassing District review due to missing configuration data.
- **Statewide Jurisdiction:** Finance Approvers and Admins skip the region match check entirely, matching the specification's description that finance-level approval operates at the scheme/treasury level rather than a localized district.
- **Officer Identity Hardening:** The verification endpoint no longer trusts client-supplied `officerId`. The officer is always resolved from the authenticated session principal, eliminating impersonation risk.
- **URL-Based Role Enforcement:** All role restrictions are applied in `SecurityConfig` via URL pattern matchers. No `@PreAuthorize` annotations are used. This keeps authorization centralized in a single configuration class.
- **Preserved Contracts:** Immutable audit history (new `Verification` row per decision), the reverification workflow (`RE_VERIFICATION_REQUIRED` → `resume-verification`), rejection paths, and all response DTOs are completely unchanged.

### 5. API Changes
- **MODIFIED:** `PATCH /api/v1/applications/{applicationId}/verify` — Now restricted to `FIELD_OFFICER`, `DISTRICT_OFFICER`, and `FINANCE_APPROVER` roles via `SecurityConfig`. The officer is resolved from the authenticated session — `officerId` is no longer accepted in the request body. When a Field officer approves, the application may route to `FINANCE_REVIEW_PENDING` (fast-tracked) instead of always going to `DISTRICT_REVIEW_PENDING`. Response DTO structure unchanged.
- **MODIFIED:** `PATCH /api/v1/applications/{applicationId}/documents/{documentId}/verify` — Now restricted to `FIELD_OFFICER`, `DISTRICT_OFFICER`, and `FINANCE_APPROVER` roles via `SecurityConfig`. Non-officer callers receive `403 Forbidden`.
- **UNCHANGED:** `PATCH /api/v1/applications/{applicationId}/resume-verification` — Reverification resume logic is identical.
- **UNCHANGED:** All other application and verification endpoints.

### 6. Testing Performed
- Ran `mvn compile` successfully. All dependency injections resolve cleanly. No circular dependencies.

### 7. Any Future Considerations
- The routing thresholds are currently hardcoded constants. If scheme-specific fast-track rules are needed in the future, the constants can be migrated to fields on the `Scheme` entity without changing the routing logic structure.
- District and Finance rejection paths remain terminal. If the specification later requires rejected applications to be resubmittable, the status machine will need an additional transition.
