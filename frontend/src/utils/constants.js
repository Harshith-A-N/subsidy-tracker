export const ROLES = {
  BENEFICIARY: 'BENEFICIARY',
  FIELD_OFFICER: 'FIELD_OFFICER',
  DISTRICT_OFFICER: 'DISTRICT_OFFICER',
  FINANCE_APPROVER: 'FINANCE_APPROVER',
  ADMIN: 'ADMIN',
};

export const ROLE_INFO = {
  BENEFICIARY: { label: 'Beneficiary', badge: 'neutral', icon: 'User' },
  FIELD_OFFICER: { label: 'Field Officer', badge: 'primary', icon: 'Search' },
  DISTRICT_OFFICER: { label: 'District Officer', badge: 'warning', icon: 'FileText' },
  FINANCE_APPROVER: { label: 'Finance Approver', badge: 'success', icon: 'Landmark' },
  ADMIN: { label: 'System Administrator', badge: 'danger', icon: 'ShieldCheck' },
};

export const APP_STATUS_INFO = {
  DRAFT: { badge: 'neutral', label: 'Draft', stage: 0, desc: 'Not yet submitted' },
  SUBMITTED: { badge: 'primary', label: 'Submitted', stage: 1, desc: 'Awaiting eligibility evaluation' },
  ELIGIBILITY_PENDING: { badge: 'warning', label: 'Eligibility Check', stage: 1, desc: 'Evaluating criteria' },
  ELIGIBLE: { badge: 'success', label: 'Eligible', stage: 1, desc: 'Passed automated eligibility' },
  NOT_ELIGIBLE: { badge: 'danger', label: 'Not Eligible', stage: 1, desc: 'Criteria not met', rejected: true },
  MANUAL_REVIEW_REQUIRED: { badge: 'warning', label: 'Manual Review', stage: 1, desc: 'Flagged for officer review' },
  FIELD_VERIFICATION_PENDING: { badge: 'warning', label: 'Field Verification Pending', stage: 2, desc: 'Awaiting field officer inspection' },
  FIELD_APPROVED: { badge: 'success', label: 'Field Approved', stage: 2, desc: 'Field officer verified' },
  FIELD_REJECTED: { badge: 'danger', label: 'Field Rejected', stage: 2, desc: 'Rejected during field verification', rejected: true },
  DISTRICT_REVIEW_PENDING: { badge: 'primary', label: 'District Review Pending', stage: 3, desc: 'Awaiting District Officer review' },
  DISTRICT_APPROVED: { badge: 'success', label: 'District Approved', stage: 3, desc: 'Sanction order approved' },
  DISTRICT_REJECTED: { badge: 'danger', label: 'District Rejected', stage: 3, desc: 'Rejected at district level', rejected: true },
  FINANCE_REVIEW_PENDING: { badge: 'warning', label: 'Finance Review Pending', stage: 4, desc: 'Awaiting finance approver' },
  FINANCE_APPROVED: { badge: 'success', label: 'Finance Approved', stage: 4, desc: 'Disbursement authorized' },
  FINANCE_REJECTED: { badge: 'danger', label: 'Finance Rejected', stage: 4, desc: 'Rejected by finance', rejected: true },
  RE_VERIFICATION_REQUIRED: { badge: 'warning', label: 'Re-Verification Required', stage: 2, desc: 'Sent back for re-check' },
  READY_FOR_DISBURSEMENT: { badge: 'primary', label: 'Ready for Disbursement', stage: 5, desc: 'Disbursement schedule active' },
  DISBURSED: { badge: 'success', label: 'Disbursed', stage: 5, desc: 'Direct Benefit Transfer completed', terminal: true },
  COMPLETED: { badge: 'success', label: 'Completed', stage: 5, desc: 'Grant fully completed', terminal: true },
};

export const BENEFICIARY_CATEGORIES = [
  'FARMER',
  'MSME',
  'STUDENT',
  'ARTISAN',
  'WOMEN_ENTREPRENEUR',
  'SENIOR_CITIZEN',
  'SC_ST',
  'GENERAL',
];

export const INDIAN_STATES = [
  'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh', 'Goa', 'Gujarat',
  'Haryana', 'Himachal Pradesh', 'Jharkhand', 'Karnataka', 'Kerala', 'Madhya Pradesh',
  'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram', 'Nagaland', 'Odisha', 'Punjab',
  'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura', 'Uttar Pradesh',
  'Uttarakhand', 'West Bengal', 'Delhi', 'Jammu and Kashmir', 'Ladakh'
];
