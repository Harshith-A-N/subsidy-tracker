# Project Progress Tracker

_Update this file at the start of each week/milestone so everyone can see current status at a glance._

## Milestone 1 (Weeks 1–2): Environment Setup & Disbursement Process Design

### Status: In Progress

**Done (Person A):**
- Project skeleton, package structure per module
- 6 shared entities: `User`, `Beneficiary`, `Scheme`, `SchemeSlab`, `RegionalBudget`, `Application`
- `application.properties` + local DB config pattern
- Verified: app starts, Hibernate creates all 6 tables in MySQL
- PR open: `feature/shared-entities-setup` → `dev` — [paste PR link here]

**This week's goals for B, C, D:**

| Person | Module | Task |
|---|---|---|
| B | Module 2 | Reviewed `Application` + `ApplicationStatus`. Designed eligibility scoring approach, verification workflow, application lifecycle states, and routing logic between field → district → finance verification stages. Documentation added for Module 2 design. |
| C | Module 3 | Review `Application` + `SchemeSlab`. Sketch a disbursement milestone entity (e.g., `application`, `milestoneName`, `dueDate`, `amount`, `isCompleted`). |
| D | Module 4 | Review `RegionalBudget`. Sketch what reports/dashboards will be needed (scheme-wise totals, region utilization %). Research PDF/Excel export libraries for later. |

**Milestone 1 checklist:**
- [x] Shared entities PR merged into `dev`
- [x] Everyone able to pull `dev`, run the app locally, and see all 6 tables
- [x] B completed Module 2 design documentation (eligibility scoring, verification workflow, application lifecycle, routing logic)
- [ ] C and D complete their respective module design sketches
- [ ] Team regroups before end of Week 2 to review sketches together before writing heavy code

---

## Milestone 2 (Weeks 3–4): Eligibility Scoring & Verification Workflow Development

Planned:
- Implement eligibility scoring engine
- Implement multi-level verification workflow
- Add application routing logic
- Develop approval, rejection, and re-verification flows

## Milestone 3 (Weeks 5–6): Disbursement & Reporting Development
_Not started yet._

## Milestone 4 (Weeks 7–8): Security, Integration & Deployment
_Not started yet._