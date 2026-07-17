# Design Decisions

This file explains a couple of decisions we made where the original
docs didn't give a clear rule.

---

## 1. NOT_ELIGIBLE vs MANUAL_REVIEW_REQUIRED

**Why this was needed:** eligibility-rules.md mentions both outcomes
but never says which failure leads to which one.

**Decision:**
- **NOT_ELIGIBLE** = income or category fails. These are simple checks
  against stored data, no judgment needed, so auto-reject.
- **MANUAL_REVIEW_REQUIRED** = documents missing/unclear, identity
  verification unclear, or region doesn't match properly. These need a
  human to check, so they go for manual review instead of auto-reject.

---

## 2. Re-verification allowed at any stage

**Why this was needed:** verification-workflow.md explains the
re-verification flow but doesn't say which officer (Field/District/
Finance) is allowed to trigger it.

**Decision:** Any stage — Field, District, or Finance — can send an
application back for re-verification, not just Field.

**Reason:** A problem might only be noticed at a later stage (e.g.
Finance finds a missing budget doc). Restricting it to Field-only
would force a full rejection instead of just asking for a fix.
No enum/schema change needed — this is just a rule for the routing
logic to follow later.