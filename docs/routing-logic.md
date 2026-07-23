Application Routing Logic
Purpose

The Routing Engine automatically forwards applications to the correct officer based on the current workflow stage.

Workflow
Beneficiary submits application
↓
Eligibility Check
↓
Eligible?
├── No
│     └── Reject / Manual Review
└── Yes
↓
Assign to Field Officer
↓
Field Officer Decision
├── Reject
│     └── End Process
├── Re-verification
│     └── Return to Beneficiary
└── Approve
↓
Assign to District Officer
↓
District Officer Decision
├── Reject
│     └── End Process
└── Approve
↓
Assign to Finance Officer
↓
Finance Approval
↓
Ready for Disbursement
Routing Principles
Applications are automatically assigned based on workflow stage.
Field/District Officer assignment also considers the officer's assigned region — an application is routed to the officer whose region matches the beneficiary's region (via Application.beneficiary), not to any officer at that stage. (See docs/regional-hierarchy.md for the full reasoning behind region scoping.)
Rejected applications stop the workflow.
Re-verification returns the application to the beneficiary.
Approved applications move to the next officer automatically.