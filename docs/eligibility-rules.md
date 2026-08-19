# Eligibility Scoring Rules

## Purpose

The Eligibility Engine evaluates whether a beneficiary qualifies for a selected government scheme based on predefined rules.

## Sample Evaluation Criteria

| Criteria | Description |
|----------|-------------|
| Income | Must satisfy scheme income limit |
| Category | Must belong to eligible beneficiary category |
| Region | Applicant must belong to eligible district/state |
| Documents | All mandatory documents must be uploaded |
| Identity | Identity verification must be successful |

## Eligibility Result

The system performs rule validation and assigns one of the following results:

- Eligible
- Not Eligible
- Requires Manual Review

If income or category fails, reject outright (NOT_ELIGIBLE). If documents or identity are incomplete/unclear, flag for manual review (MANUAL_REVIEW_REQUIRED).

## Output

If all required conditions are satisfied:

Status:
ELIGIBLE

Otherwise:

Status:
NOT_ELIGIBLE
or
MANUAL_REVIEW_REQUIRED

> Note:
Actual scoring rules are configurable and may vary depending on the government scheme.