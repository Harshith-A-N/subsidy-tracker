Regional Hierarchy Structure
1. Overview

The project brief (Milestone 1) asks us to define a "regional hierarchy structure" alongside the disbursement process architecture. This term is ambiguous on its own — it could mean:

Organizational hierarchy: which staff role has authority over which region (e.g., a District Officer approving only applications from their own district), or
Geographic hierarchy: how regions physically nest (State → District → Block/Taluk), independent of who approves what.

This doc resolves that ambiguity for our system.

2. Decision

We are implementing organizational (approval) scoping only, not geographic nesting.

Concretely:

A flat region field (String) is added to User for roles whose responsibilities are region-specific (Field Officer, District Officer). This mirrors the existing RegionalBudget.regionName field — same shape, same simplification.
No new Region entity is introduced. Regions remain flat, unrelated strings — there is no parent/child relationship modeled between them (e.g., no representation of "District X belongs to State Y").
RegionalBudget is unchanged. It already models region as a flat field per (scheme, region) pair, which is consistent with this decision.
3. Rationale

Three sources of evidence were reviewed before this decision:

docs/verification-workflow.md describes the approval chain (Field Officer → District Officer → Finance Officer) purely as a sequence of roles. It does not describe routing an application to a specific officer based on geography. This means the documented workflow, as written, does not depend on geographic nesting.
Role naming implies scoping, even if undocumented. A role named "District Officer" only makes sense if that officer is responsible for a specific district — otherwise the role would just be "Level 2 Approver." This is why organizational scoping (region ↔ officer) is still a real requirement, despite not being explicit in verification-workflow.md. It is an assumption made explicit here rather than left implicit in code.
The project brief separates region-as-reporting-dimension from region-as-disbursement-mechanic. Module 3 (Staged Disbursement & Compliance Milestone Tracking) never references region — disbursement stages are driven by compliance milestones (documentation, verification, utilization proof), not by geography. Region only appears in Module 4 (region-wise fund utilization dashboards) and in the project outcomes ("inconsistent approval standards across regions"). This confirms region is a tracking/reporting/scoping dimension, not something that changes how disbursement staging itself works — so disbursement architecture (Section 2 of this milestone's remaining work) does not need to model regional nesting either.

Given all three points, a flat region field satisfies every actual requirement we can currently point to. Geographic nesting would add schema complexity (a self-referential Region entity, parent/child queries) with no requirement currently justifying it.

4. Impact on Existing Entities
   Entity	Change
   User	New field: region (String, nullable — only meaningful for FIELD_OFFICER / DISTRICT_OFFICER roles; ADMIN and FINANCE_APPROVER may operate across all regions)
   RegionalBudget	No change — already flat, already correct
   Beneficiary	No change — region is not currently modeled on Beneficiary; if approval scoping needs to match beneficiary to officer by region, this should be revisited (see Open Question below)
5. Future Improvement (Deferred, Documented Simplification)

If the system later needs true geographic nesting (e.g., State-level dashboards that roll up District-level budgets, or an officer hierarchy where a State Officer can see all Districts beneath them), the textbook-correct approach would be a self-referential Region entity:

Region { id, name, parentRegion (nullable, @ManyToOne self-reference) }

User.region and RegionalBudget.regionName would then become foreign keys into Region instead of flat strings. This is deliberately deferred, in the same spirit as the Scheme.allowedCategories CSV simplification — it is a known, documented tradeoff, not an oversight.

6. Open Question / Assumption to Verify

This decision assumes officer-to-region scoping is required (Section 3, point 2) based on role naming rather than explicit confirmation in docs/routing-logic.md. If routing-logic.md is reviewed later and contradicts this (e.g., explicitly states any officer can act on any application regardless of region), this doc should be revisited.