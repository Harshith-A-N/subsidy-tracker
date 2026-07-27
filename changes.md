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
