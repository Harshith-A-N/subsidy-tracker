-- Automatic Database Seeder for Subsidy Tracker
-- Seeds exactly 1 user per role on initial database creation
-- Password for all pre-seeded accounts is: admin123

INSERT INTO users (id, email, password, full_name, role, region) VALUES
(1, 'admin@govgrant.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOUVGkqRzgVymGe07xd0D1HPH6p568e6e', 'System Admin', 'ADMIN', 'ALL'),
(2, 'fa@govgrant.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOUVGkqRzgVymGe07xd0D1HPH6p568e6e', 'Finance Approver', 'FINANCE_APPROVER', 'HQ'),
(3, 'do@govgrant.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOUVGkqRzgVymGe07xd0D1HPH6p568e6e', 'District Officer', 'DISTRICT_OFFICER', 'District 1'),
(4, 'fo@govgrant.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOUVGkqRzgVymGe07xd0D1HPH6p568e6e', 'Field Officer', 'FIELD_OFFICER', 'District 1'),
(5, 'ramesh@example.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOUVGkqRzgVymGe07xd0D1HPH6p568e6e', 'Ramesh Kumar', 'BENEFICIARY', 'District 1');
