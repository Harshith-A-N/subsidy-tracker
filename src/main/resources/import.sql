-- Automatic Database Seeder for Subsidy Tracker
-- Seeds exactly 1 user per role on initial database creation
-- Password for all pre-seeded accounts is: 123456

INSERT IGNORE INTO users (id, email, password, full_name, role, region) VALUES
(1, 'admin@gmail.com', '$2b$12$h7Hs18z1eq0QqqGns1SW5Os9vKl3NucksVDecrJjliRMfOd0lD1vK', 'System Admin', 'ADMIN', 'ALL'),
(2, 'fa@gmail.com', '$2b$12$h7Hs18z1eq0QqqGns1SW5Os9vKl3NucksVDecrJjliRMfOd0lD1vK', 'Finance Officer', 'FINANCE_APPROVER', 'HQ'),
(3, 'd0@gmail.com', '$2b$12$h7Hs18z1eq0QqqGns1SW5Os9vKl3NucksVDecrJjliRMfOd0lD1vK', 'District Officer', 'DISTRICT_OFFICER', 'District 1'),
(4, 'fo@gmail.com', '$2b$12$h7Hs18z1eq0QqqGns1SW5Os9vKl3NucksVDecrJjliRMfOd0lD1vK', 'Field Officer', 'FIELD_OFFICER', 'District 1'),
(5, 'me@gmail.com', '$2b$12$h7Hs18z1eq0QqqGns1SW5Os9vKl3NucksVDecrJjliRMfOd0lD1vK', 'Beneficiary User', 'BENEFICIARY', 'District 1');
