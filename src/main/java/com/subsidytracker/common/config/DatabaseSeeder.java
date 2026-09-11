package com.subsidytracker.common.config;

import com.subsidytracker.beneficiary.repository.BeneficiaryRepository;
import com.subsidytracker.common.entity.*;
import com.subsidytracker.common.enums.*;
import com.subsidytracker.common.repository.AuditLogRepository;
import com.subsidytracker.disbursement.entity.ApplicationDisbursementSchedule;
import com.subsidytracker.disbursement.entity.DisbursementMilestone;
import com.subsidytracker.disbursement.entity.DisbursementPlan;
import com.subsidytracker.disbursement.entity.DisbursementStage;
import com.subsidytracker.disbursement.repository.ApplicationDisbursementScheduleRepository;
import com.subsidytracker.disbursement.repository.DisbursementMilestoneRepository;
import com.subsidytracker.disbursement.repository.DisbursementPlanRepository;
import com.subsidytracker.disbursement.repository.DisbursementStageRepository;
import com.subsidytracker.eligibility.repository.ApplicationRepository;
import com.subsidytracker.eligibility.repository.DocumentRepository;
import com.subsidytracker.eligibility.repository.UserRepository;
import com.subsidytracker.eligibility.repository.VerificationRepository;
import com.subsidytracker.scheme.repository.RegionalBudgetRepository;
import com.subsidytracker.scheme.repository.SchemeRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;
import com.subsidytracker.security.entity.OfficerRegistrationRequest;
import com.subsidytracker.security.repository.OfficerRegistrationRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@SuppressWarnings("unused")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seeding.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    public static final String DEFAULT_CLOUDINARY_DOC_URL =
            "https://res.cloudinary.com/dubkk5bwa/image/upload/v1789099923/Subsidy%20Tracker/documents/gn0c3ygwnet5ashbd7z5.png";

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final UserRepository userRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchemeRepository schemeRepository;
    private final SchemeSlabRepository schemeSlabRepository;
    private final RegionalBudgetRepository regionalBudgetRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final DisbursementPlanRepository disbursementPlanRepository;
    private final DisbursementStageRepository disbursementStageRepository;
    private final ApplicationDisbursementScheduleRepository scheduleRepository;
    private final DisbursementMilestoneRepository milestoneRepository;
    private final VerificationRepository verificationRepository;
    private final OfficerRegistrationRequestRepository officerRegistrationRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(DataSource dataSource,
                          PlatformTransactionManager transactionManager,
                          UserRepository userRepository,
                          BeneficiaryRepository beneficiaryRepository,
                          SchemeRepository schemeRepository,
                          SchemeSlabRepository schemeSlabRepository,
                          RegionalBudgetRepository regionalBudgetRepository,
                          ApplicationRepository applicationRepository,
                          DocumentRepository documentRepository,
                          DisbursementPlanRepository disbursementPlanRepository,
                          DisbursementStageRepository disbursementStageRepository,
                          ApplicationDisbursementScheduleRepository scheduleRepository,
                          DisbursementMilestoneRepository milestoneRepository,
                          VerificationRepository verificationRepository,
                          OfficerRegistrationRequestRepository officerRegistrationRequestRepository,
                          AuditLogRepository auditLogRepository,
                          PasswordEncoder passwordEncoder) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.schemeRepository = schemeRepository;
        this.schemeSlabRepository = schemeSlabRepository;
        this.regionalBudgetRepository = regionalBudgetRepository;
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.disbursementPlanRepository = disbursementPlanRepository;
        this.disbursementStageRepository = disbursementStageRepository;
        this.scheduleRepository = scheduleRepository;
        this.milestoneRepository = milestoneRepository;
        this.verificationRepository = verificationRepository;
        this.officerRegistrationRequestRepository = officerRegistrationRequestRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            ensureDemoBeneficiaryExists();
            if (userRepository.count() > 0 && schemeRepository.count() > 0) {
                log.info("Database is already seeded ({} users, {} schemes present). Skipping full re-seeding.",
                        userRepository.count(), schemeRepository.count());
                ensureOfficerRegions();
                ensureFieldVerificationsExist();
                ensureApplicationDocumentsExist();
                return;
            }
            log.info("Database is empty. Running initial database seeder...");
            truncateDatabase();
            new TransactionTemplate(transactionManager).execute(status -> {
                doSeed();
                return null;
            });
            ensureApplicationDocumentsExist();
        } catch (Exception e) {
            log.error("DatabaseSeeder encountered an error — application will still run. Cause: {}", e.getMessage(), e);
        }
    }

    private void ensureDemoBeneficiaryExists() {
        new TransactionTemplate(transactionManager).execute(status -> {
            if (userRepository.findByEmail("me@gmail.com").isEmpty()) {
                log.info("Demo beneficiary 'me@gmail.com' not found. Creating demo beneficiary account...");
                User meUser = seedUser("me@gmail.com", "Demo Beneficiary Citizen", Role.BENEFICIARY, "Maharashtra");
                Beneficiary bDemo = seedBeneficiary(meUser, "Demo Beneficiary Citizen", "999988887777", "9876543200",
                        "Shivaji Nagar, Pune, Maharashtra", BeneficiaryCategory.GENERAL, "Maharashtra", new BigDecimal("150000.00"));

                var schemes = schemeRepository.findAll();
                if (schemes.size() >= 2) {
                    Scheme s1 = schemes.get(0); // PM Surya Ghar & Solar Agri-Pump
                    Scheme s2 = schemes.get(1); // Agricultural Mechanization
                    seedApp(bDemo, s1, ApplicationStatus.READY_FOR_DISBURSEMENT, 92.5, LocalDate.now().minusDays(10), "Sanctioned solar subsidy application ready for tranche release.");
                    seedApp(bDemo, s2, ApplicationStatus.DISBURSED, 95.0, LocalDate.now().minusMonths(2), "Completed farm mechanization subsidy.");
                } else if (!schemes.isEmpty()) {
                    Scheme s1 = schemes.get(0);
                    seedApp(bDemo, s1, ApplicationStatus.READY_FOR_DISBURSEMENT, 92.5, LocalDate.now().minusDays(10), "Sanctioned subsidy application ready for tranche release.");
                }
                log.info("Demo beneficiary 'me@gmail.com' (password: 123456) created successfully across distinct schemes.");
            }
            return null;
        });
    }

    private void ensureOfficerRegions() {
        new TransactionTemplate(transactionManager).execute(status -> {
            userRepository.findByEmail("fo@gmail.com").ifPresent(fo -> {
                if (!"Maharashtra".equalsIgnoreCase(fo.getRegion())) {
                    fo.setRegion("Maharashtra");
                    userRepository.save(fo);
                }
            });
            userRepository.findByEmail("do@gmail.com").ifPresent(dOff -> {
                if (!"Maharashtra".equalsIgnoreCase(dOff.getRegion())) {
                    dOff.setRegion("Maharashtra");
                    userRepository.save(dOff);
                }
            });
            return null;
        });
    }

    /**
     * Backfills FIELD-level Verification records for any FIELD_APPROVED application
     * that is missing one. Runs every startup so existing seeded DBs are also covered.
     */
    private void ensureFieldVerificationsExist() {
        new TransactionTemplate(transactionManager).execute(status -> {
            var foUserOpt = userRepository.findByEmail("fo@gmail.com");
            if (foUserOpt.isEmpty()) return null;
            User foUser = foUserOpt.get();

            var fieldApprovedApps = applicationRepository.findByStatus(ApplicationStatus.FIELD_APPROVED);
            for (Application app : fieldApprovedApps) {
                boolean hasFieldVerification = verificationRepository
                        .findTopByApplicationIdOrderByVerificationDateDesc(app.getId())
                        .filter(v -> v.getLevel() == VerificationLevel.FIELD)
                        .isPresent();
                if (!hasFieldVerification) {
                    log.info("Backfilling FIELD verification record for application #{}", app.getId());
                    seedVerification(app, foUser, VerificationLevel.FIELD, VerificationDecision.APPROVED,
                            "On-site ground inspection completed. Field conditions verified.",
                            app.getSubmissionDate() != null
                                    ? app.getSubmissionDate().atTime(10, 30)
                                    : java.time.LocalDateTime.now().minusDays(10));
                }
            }
            return null;
        });
    }

    /**
     * Ensures all applications have required KYC and tranche utilization documents
     * backed by real Cloudinary asset URLs, with verification statuses matching the application state.
     */
    private void ensureApplicationDocumentsExist() {
        new TransactionTemplate(transactionManager).execute(status -> {
            var allApps = applicationRepository.findAll();
            int backfilledCount = 0;
            for (Application app : allApps) {
                var existingDocs = documentRepository.findByApplicationId(app.getId());
                var existingTypes = existingDocs.stream()
                        .map(Document::getDocumentType)
                        .collect(java.util.stream.Collectors.toSet());

                // Required document types for the scheme
                String reqDocsStr = app.getScheme() != null && app.getScheme().getRequiredDocuments() != null
                        ? app.getScheme().getRequiredDocuments()
                        : "Aadhaar Card, Land Record, Bank Passbook";

                String[] docTypes = reqDocsStr.split(",");
                for (String docTypeRaw : docTypes) {
                    String docType = docTypeRaw.trim().toUpperCase().replace(" ", "_");
                    if (docType.isEmpty()) continue;
                    if (!existingTypes.contains(docType)) {
                        Document doc = new Document();
                        doc.setApplication(app);
                        doc.setDocumentType(docType);
                        doc.setFilePath(DEFAULT_CLOUDINARY_DOC_URL);
                        doc.setUploadedAt(app.getSubmissionDate() != null
                                ? app.getSubmissionDate().atTime(10, 0)
                                : LocalDateTime.now().minusDays(15));

                        // Set verification status consistent with application lifecycle
                        if (app.getStatus() == ApplicationStatus.FIELD_APPROVED
                                || app.getStatus() == ApplicationStatus.DISTRICT_REVIEW_PENDING
                                || app.getStatus() == ApplicationStatus.DISTRICT_APPROVED
                                || app.getStatus() == ApplicationStatus.FINANCE_REVIEW_PENDING
                                || app.getStatus() == ApplicationStatus.FINANCE_APPROVED
                                || app.getStatus() == ApplicationStatus.READY_FOR_DISBURSEMENT
                                || app.getStatus() == ApplicationStatus.DISBURSED
                                || app.getStatus() == ApplicationStatus.COMPLETED) {
                            doc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
                            doc.setRemarks("Biometrics and physical copy verified during field inspection.");
                        } else if (app.getStatus() == ApplicationStatus.FIELD_REJECTED
                                || app.getStatus() == ApplicationStatus.DISTRICT_REJECTED
                                || app.getStatus() == ApplicationStatus.FINANCE_REJECTED) {
                            if (docType.contains("LAND") || docType.contains("INCOME")) {
                                doc.setVerificationStatus(DocumentVerificationStatus.REJECTED);
                                doc.setRemarks("Document validation failed - mismatched registry credentials.");
                            } else {
                                doc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
                                doc.setRemarks("Document verified.");
                            }
                        } else {
                            doc.setVerificationStatus(DocumentVerificationStatus.PENDING);
                        }

                        documentRepository.save(doc);
                        existingTypes.add(docType);
                        backfilledCount++;
                    }
                }

                // Check milestone stages for utilization proofs
                var schedules = scheduleRepository.findByApplicationId(app.getId());
                for (var sch : schedules) {
                    if (sch.getStatus() == DisbursementScheduleStatus.RELEASED) {
                        boolean hasProof = documentRepository.existsByApplicationIdAndStageId(app.getId(), sch.getStage().getId());
                        if (!hasProof) {
                            Document proofDoc = new Document();
                            proofDoc.setApplication(app);
                            proofDoc.setStage(sch.getStage());
                            proofDoc.setDocumentType("STAGE_UTILIZATION_PROOF");
                            proofDoc.setFilePath(DEFAULT_CLOUDINARY_DOC_URL);
                            proofDoc.setUploadedAt(LocalDateTime.now().minusDays(5));
                            proofDoc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
                            proofDoc.setRemarks("Geo-tagged stage installation & expenditure utilization proof verified.");
                            documentRepository.save(proofDoc);
                            backfilledCount++;
                        }
                    }
                }
            }
            if (backfilledCount > 0) {
                log.info("Backfilled {} missing application documents with Cloudinary asset links and verified statuses.", backfilledCount);
            }
            return null;
        });
    }

    public void doSeed() {
        log.info("========== SEEDING DYNAMIC DATABASE DATA ==========");

        // 1. Core Official Users
        User adminUser = seedUser("admin@gmail.com", "System Administrator", Role.ADMIN, "ALL");
        User faUser = seedUser("fa@gmail.com", "Finance Officer", Role.FINANCE_APPROVER, "All Regions");
        User doUser = seedUser("do@gmail.com", "District Officer", Role.DISTRICT_OFFICER, "Maharashtra");
        User foUser = seedUser("fo@gmail.com", "Field Inspector", Role.FIELD_OFFICER, "Maharashtra");

        // 2. Demo Beneficiary Account (for 1-Click login me@gmail.com / 123456)
        User meUser = seedUser("me@gmail.com", "Demo Beneficiary Citizen", Role.BENEFICIARY, "Maharashtra");
        Beneficiary bDemo = seedBeneficiary(meUser, "Demo Beneficiary Citizen", "999988887777", "9876543200",
                "Shivaji Nagar, Pune, Maharashtra", BeneficiaryCategory.GENERAL, "Maharashtra", new BigDecimal("150000.00"));

        // 3. Beneficiaries Across Diverse Social Categories & States
        User u1 = seedUser("ramesh@gmail.com", "Ramesh Kumar Sharma", Role.BENEFICIARY, "Maharashtra");
        Beneficiary b1 = seedBeneficiary(u1, "Ramesh Kumar Sharma", "123456789012", "9876543210",
                "Village Khed, Pune, Maharashtra", BeneficiaryCategory.OBC, "Maharashtra", new BigDecimal("180000.00"));

        User u2 = seedUser("sunita@gmail.com", "Sunita Devi", Role.BENEFICIARY, "Uttar Pradesh");
        Beneficiary b2 = seedBeneficiary(u2, "Sunita Devi", "234567890123", "9876543211",
                "Sector 14, Lucknow, Uttar Pradesh", BeneficiaryCategory.SC, "Uttar Pradesh", new BigDecimal("95000.00"));

        User u3 = seedUser("suresh@gmail.com", "Suresh Patel", Role.BENEFICIARY, "Gujarat");
        Beneficiary b3 = seedBeneficiary(u3, "Suresh Patel", "345678901234", "9876543212",
                "GIDC Industrial Area, Ahmedabad, Gujarat", BeneficiaryCategory.GENERAL, "Gujarat", new BigDecimal("320000.00"));

        User u4 = seedUser("anita@gmail.com", "Anita Deshmukh", Role.BENEFICIARY, "Maharashtra");
        Beneficiary b4 = seedBeneficiary(u4, "Anita Deshmukh", "456789012345", "9876543213",
                "Wardha Road, Nagpur, Maharashtra", BeneficiaryCategory.EWS, "Maharashtra", new BigDecimal("140000.00"));

        User u5 = seedUser("basav@gmail.com", "Basavaraj Patil", Role.BENEFICIARY, "Karnataka");
        Beneficiary b5 = seedBeneficiary(u5, "Basavaraj Patil", "567890123456", "9876543214",
                "Hubli Rural District, Karnataka", BeneficiaryCategory.OBC, "Karnataka", new BigDecimal("210000.00"));

        User u6 = seedUser("rajesh@gmail.com", "Rajesh Vishwakarma", Role.BENEFICIARY, "Rajasthan");
        Beneficiary b6 = seedBeneficiary(u6, "Rajesh Vishwakarma", "678901234567", "9876543215",
                "Amber Crafts Cluster, Jaipur, Rajasthan", BeneficiaryCategory.ST, "Rajasthan", new BigDecimal("110000.00"));

        User u7 = seedUser("priya@gmail.com", "Priya Lakshmi", Role.BENEFICIARY, "Tamil Nadu");
        Beneficiary b7 = seedBeneficiary(u7, "Priya Lakshmi", "789012345678", "9876543216",
                "Anna Nagar West, Chennai, Tamil Nadu", BeneficiaryCategory.GENERAL, "Tamil Nadu", new BigDecimal("240000.00"));

        User u8 = seedUser("farooq@gmail.com", "Mohammed Farooq", Role.BENEFICIARY, "Madhya Pradesh");
        Beneficiary b8 = seedBeneficiary(u8, "Mohammed Farooq", "890123456789", "9876543217",
                "Old City Market, Bhopal, Madhya Pradesh", BeneficiaryCategory.OBC, "Madhya Pradesh", new BigDecimal("165000.00"));

        User u9 = seedUser("meena@gmail.com", "Meenakshi Sundaram", Role.BENEFICIARY, "Tamil Nadu");
        Beneficiary b9 = seedBeneficiary(u9, "Meenakshi Sundaram", "901234567890", "9876543218",
                "Sellur, Madurai, Tamil Nadu", BeneficiaryCategory.SC, "Tamil Nadu", new BigDecimal("85000.00"));

        User u10 = seedUser("vikram@gmail.com", "Vikram Rathore", Role.BENEFICIARY, "Rajasthan");
        Beneficiary b10 = seedBeneficiary(u10, "Vikram Rathore", "112233445566", "9876543219",
                "Ratanada, Jodhpur, Rajasthan", BeneficiaryCategory.GENERAL, "Rajasthan", new BigDecimal("290000.00"));

        User u11 = seedUser("pooja@gmail.com", "Pooja Verma", Role.BENEFICIARY, "Uttar Pradesh");
        Beneficiary b11 = seedBeneficiary(u11, "Pooja Verma", "223344556677", "9876543220",
                "Civil Lines, Varanasi, Uttar Pradesh", BeneficiaryCategory.EWS, "Uttar Pradesh", new BigDecimal("120000.00"));

        User u12 = seedUser("gurpreet@gmail.com", "Gurpreet Singh", Role.BENEFICIARY, "Gujarat");
        Beneficiary b12 = seedBeneficiary(u12, "Gurpreet Singh", "334455667788", "9876543221",
                "Sayajigunj, Vadodara, Gujarat", BeneficiaryCategory.GENERAL, "Gujarat", new BigDecimal("380000.00"));

        log.info("12 diverse beneficiaries and official staff seeded.");

        // 4. Schemes Across Multiple Public Sectors
        Scheme s1 = seedScheme("PM Surya Ghar & Solar Agri-Pump Subsidy",
                "Capital grant assistance for standalone solar-powered irrigation pumps to replace diesel and reduce agricultural power costs.",
                BigDecimal.ZERO, new BigDecimal("400000.00"), "GENERAL,OBC,SC,ST,EWS", "Aadhaar Card, Land Record, Income Certificate");

        Scheme s2 = seedScheme("MSME Technology Modernization Grant",
                "Credit-linked capital subsidy for micro and small enterprises upgrading to automated green machinery and CNC systems.",
                BigDecimal.ZERO, new BigDecimal("1500000.00"), "GENERAL,OBC,SC,ST,EWS", "Aadhaar Card, Udyam Registration, Bank Passbook, GST Certificate");

        Scheme s3 = seedScheme("Pradhan Mantri Rural Housing Subsidy",
                "Permanent pucca housing construction assistance for BPL and marginalized rural families across India.",
                BigDecimal.ZERO, new BigDecimal("200000.00"), "GENERAL,OBC,SC,ST,EWS", "Aadhaar Card, BPL Card, Land Title Patta, Bank Passbook");

        Scheme s4 = seedScheme("Women Agri-Entrepreneurship Incentive Scheme",
                "Direct seed funding and technical toolkit grants for rural women farmer producer groups and agribusiness start-ups.",
                BigDecimal.ZERO, new BigDecimal("500000.00"), "GENERAL,OBC,SC,ST,EWS", "Aadhaar Card, Income Certificate, Land Record, Bank Passbook");

        Scheme s5 = seedScheme("National Higher Education Merit Scholarship",
                "Direct tuition assistance for economically disadvantaged undergraduate students pursuing professional STEM degree courses.",
                BigDecimal.ZERO, new BigDecimal("350000.00"), "GENERAL,OBC,SC,ST,EWS", "Aadhaar Card, Academic Marksheet, College Admission Letter, Income Certificate");

        log.info("5 core government schemes seeded.");

        // 5. Scheme Slabs Configured for ALL Schemes Across Categories
        seedSlabsForScheme(s1, new BigDecimal("150000.00"), new BigDecimal("175000.00"), new BigDecimal("200000.00"), new BigDecimal("200000.00"), new BigDecimal("180000.00"));
        seedSlabsForScheme(s2, new BigDecimal("300000.00"), new BigDecimal("350000.00"), new BigDecimal("450000.00"), new BigDecimal("450000.00"), new BigDecimal("350000.00"));
        seedSlabsForScheme(s3, new BigDecimal("180000.00"), new BigDecimal("200000.00"), new BigDecimal("250000.00"), new BigDecimal("250000.00"), new BigDecimal("220000.00"));
        seedSlabsForScheme(s4, new BigDecimal("100000.00"), new BigDecimal("120000.00"), new BigDecimal("150000.00"), new BigDecimal("150000.00"), new BigDecimal("125000.00"));
        seedSlabsForScheme(s5, new BigDecimal("60000.00"), new BigDecimal("75000.00"), new BigDecimal("90000.00"), new BigDecimal("90000.00"), new BigDecimal("80000.00"));

        // 6. Regional Budgets Across Key States (Covering >75% WARNING and >95% CRITICAL)
        // Scheme 1: Solar Pumps
        seedRegionalBudget(s1, "Maharashtra", new BigDecimal("25000000.00"), new BigDecimal("21500000.00")); // 86% -> WARNING
        seedRegionalBudget(s1, "Gujarat", new BigDecimal("18000000.00"), new BigDecimal("9500000.00"));      // 52.8%
        seedRegionalBudget(s1, "Rajasthan", new BigDecimal("15000000.00"), new BigDecimal("6000000.00"));    // 40%
        seedRegionalBudget(s1, "Karnataka", new BigDecimal("12000000.00"), new BigDecimal("3000000.00"));    // 25%

        // Scheme 2: MSME
        seedRegionalBudget(s2, "Gujarat", new BigDecimal("30000000.00"), new BigDecimal("28800000.00"));     // 96% -> CRITICAL!
        seedRegionalBudget(s2, "Maharashtra", new BigDecimal("40000000.00"), new BigDecimal("22000000.00")); // 55%
        seedRegionalBudget(s2, "Tamil Nadu", new BigDecimal("25000000.00"), new BigDecimal("8000000.00"));   // 32%

        // Scheme 3: Rural Housing
        seedRegionalBudget(s3, "Uttar Pradesh", new BigDecimal("50000000.00"), new BigDecimal("43000000.00")); // 86% -> WARNING
        seedRegionalBudget(s3, "Madhya Pradesh", new BigDecimal("35000000.00"), new BigDecimal("14000000.00"));// 40%
        seedRegionalBudget(s3, "Rajasthan", new BigDecimal("30000000.00"), new BigDecimal("12000000.00"));     // 40%

        // Scheme 4: Women Entrepreneurship
        seedRegionalBudget(s4, "Maharashtra", new BigDecimal("20000000.00"), new BigDecimal("8000000.00"));
        seedRegionalBudget(s4, "Karnataka", new BigDecimal("15000000.00"), new BigDecimal("4500000.00"));
        seedRegionalBudget(s4, "Tamil Nadu", new BigDecimal("15000000.00"), new BigDecimal("5000000.00"));

        // Scheme 5: Education Merit Scholarship
        seedRegionalBudget(s5, "Uttar Pradesh", new BigDecimal("20000000.00"), new BigDecimal("9000000.00"));
        seedRegionalBudget(s5, "Maharashtra", new BigDecimal("20000000.00"), new BigDecimal("8500000.00"));
        seedRegionalBudget(s5, "Tamil Nadu", new BigDecimal("15000000.00"), new BigDecimal("6000000.00"));

        // 7. Multi-Stage Disbursement Plans
        DisbursementPlan plan1 = seedPlan(s1, adminUser, 4,
                new StageDef("Approval & Agreement", 1, 25, TriggerMilestone.APPLICATION_APPROVAL, 15),
                new StageDef("Pump Installation Verified", 2, 35, TriggerMilestone.GROUND_VERIFICATION, 45),
                new StageDef("Utilization & Grid Sync", 3, 30, TriggerMilestone.UTILIZATION_PROOF, 90),
                new StageDef("Project Final Closure", 4, 10, TriggerMilestone.PROJECT_CLOSURE, 120));

        DisbursementPlan plan2 = seedPlan(s2, adminUser, 3,
                new StageDef("Equipment Advance", 1, 40, TriggerMilestone.APPLICATION_APPROVAL, 20),
                new StageDef("Machinery Commissioning", 2, 40, TriggerMilestone.GROUND_VERIFICATION, 60),
                new StageDef("Commercial Output Proof", 3, 20, TriggerMilestone.UTILIZATION_PROOF, 90));

        DisbursementPlan plan3 = seedPlan(s3, adminUser, 3,
                new StageDef("Plinth & Foundation", 1, 40, TriggerMilestone.APPLICATION_APPROVAL, 30),
                new StageDef("Lintel & Roof Level", 2, 40, TriggerMilestone.GROUND_VERIFICATION, 75),
                new StageDef("Occupancy Certificate", 3, 20, TriggerMilestone.PROJECT_CLOSURE, 120));

        DisbursementPlan plan4 = seedPlan(s4, adminUser, 3,
                new StageDef("Sanction Disbursement", 1, 35, TriggerMilestone.APPLICATION_APPROVAL, 15),
                new StageDef("Asset Acquisition Proof", 2, 35, TriggerMilestone.GROUND_VERIFICATION, 45),
                new StageDef("Operational Proof", 3, 30, TriggerMilestone.UTILIZATION_PROOF, 90));

        DisbursementPlan plan5 = seedPlan(s5, adminUser, 2,
                new StageDef("Tuition Fee Grant", 1, 50, TriggerMilestone.APPLICATION_APPROVAL, 15),
                new StageDef("Semester Progression Grant", 2, 50, TriggerMilestone.UTILIZATION_PROOF, 90));

        // 8. Seed 21 Applications Covering ALL Edge Cases
        log.info("Seeding 21 applications spanning every pipeline state & edge case...");

        // Edge Case 1: DRAFT (beneficiary started application, awaiting documents)
        Application app1 = seedApp(b1, s2, ApplicationStatus.DRAFT, 0.0, LocalDate.now(), "Draft initiated by Ramesh Kumar. Awaiting document attachments.");

        // Edge Case 2: SUBMITTED (auto-scored, high eligibility)
        Application app2 = seedApp(b4, s4, ApplicationStatus.SUBMITTED, 88.0, LocalDate.of(2026, 8, 25), "Application submitted and auto-scored. Passed baseline criteria.");

        // Edge Case 3: MANUAL_REVIEW_REQUIRED (borderline score 64.5 -> tests Admin Recalculate button!)
        Application app3 = seedApp(b7, s1, ApplicationStatus.MANUAL_REVIEW_REQUIRED, 64.5, LocalDate.of(2026, 8, 18), "Borderline score (64.5). Income near threshold (₹2.4L). Manual review required by Admin.");

        // Edge Case 4: NOT_ELIGIBLE (auto-rejected on submission)
        Application app4 = seedApp(b10, s3, ApplicationStatus.NOT_ELIGIBLE, 34.0, LocalDate.of(2026, 7, 12), "Auto-rejected: declared annual income exceeds scheme ceiling limit.");

        // Edge Case 5: FIELD_VERIFICATION_PENDING (in field officer queue)
        Application app5 = seedApp(b6, s1, ApplicationStatus.FIELD_VERIFICATION_PENDING, 84.0, LocalDate.of(2026, 8, 10), "Forwarded to Junior Field Inspector for site inspection.");

        // Edge Case 6: RE_VERIFICATION_REQUIRED (sent back by District Officer with remarks)
        Application app6 = seedApp(b1, s1, ApplicationStatus.RE_VERIFICATION_REQUIRED, 76.0, LocalDate.of(2026, 7, 28), "DO Note: Discrepancy in survey map. Declared land 4.5 acres vs GIS record 2.2 acres. Ground re-inspection required.");

        // Edge Case 7: FIELD_APPROVED (field inspector verified on-site)
        Application app7 = seedApp(b4, s1, ApplicationStatus.FIELD_APPROVED, 91.0, LocalDate.of(2026, 8, 2), "Ground verification completed. Soil & water depth suitable for solar pump. Passed.");

        // Edge Case 8: FIELD_REJECTED (rejected by field officer)
        Application app8 = seedApp(b8, s1, ApplicationStatus.FIELD_REJECTED, 42.0, LocalDate.of(2026, 6, 15), "Field inspector inspection: parcel used for commercial warehouse, no farming activity observed.");

        // Edge Case 9: DISTRICT_REVIEW_PENDING (in district officer queue)
        Application app9 = seedApp(b3, s2, ApplicationStatus.DISTRICT_REVIEW_PENDING, 89.5, LocalDate.of(2026, 7, 20), "Field verification approved. Awaiting District Officer sanction review.");

        // Edge Case 10: DISTRICT_APPROVED (sanction order generated)
        Application app10 = seedApp(b5, s3, ApplicationStatus.DISTRICT_APPROVED, 92.0, LocalDate.of(2026, 7, 5), "District Officer sanction order #DO-2026-089 issued. Forwarded to Finance for treasury allocation.");

        // Edge Case 11: DISTRICT_REJECTED (rejected by district officer)
        Application app11 = seedApp(b12, s2, ApplicationStatus.DISTRICT_REJECTED, 55.0, LocalDate.of(2026, 6, 22), "Rejected by District Welfare Officer: Applicant spouse already beneficiary of Central Solar Subsidy 2024.");

        // Edge Case 12: FINANCE_REVIEW_PENDING (in finance approval queue)
        Application app12 = seedApp(b2, s3, ApplicationStatus.FINANCE_REVIEW_PENDING, 94.0, LocalDate.of(2026, 7, 14), "District sanctioned. In Finance queue awaiting treasury fund release sign-off.");

        // Edge Case 13: FINANCE_APPROVED (finance sign-off done)
        Application app13 = seedApp(b9, s4, ApplicationStatus.FINANCE_APPROVED, 93.5, LocalDate.of(2026, 6, 30), "Finance approval granted. Staged disbursement schedule activated.");

        // Edge Case 14: FINANCE_REJECTED (finance officer rejected)
        Application app14 = seedApp(b11, s3, ApplicationStatus.FINANCE_REJECTED, 58.0, LocalDate.of(2026, 6, 10), "Finance rejected: Beneficiary bank IFSC branch code invalid or account closed.");

        // Edge Case 15: READY_FOR_DISBURSEMENT (approved, schedule generated, first tranche ready)
        Application app15 = seedApp(b5, s1, ApplicationStatus.READY_FOR_DISBURSEMENT, 95.0, LocalDate.of(2026, 6, 25), "Treasury clearance complete. Stage 1 disbursement tranche ready for release.");
        seedAppSchedules(app15, plan1, new BigDecimal("175000.00"), false, false);

        // Edge Case 16: DISBURSED (Stage 1 Released, Stage 2 Pending)
        Application app16 = seedApp(b3, s1, ApplicationStatus.DISBURSED, 91.0, LocalDate.of(2026, 5, 15), "Stage 1 advance (₹43,750) released via DBT. Awaiting Stage 2 installation verification.");
        seedAppSchedules(app16, plan1, new BigDecimal("175000.00"), true, false);

        // Edge Case 17: DISBURSED with OVERDUE Compliance Milestone!
        Application app17 = seedApp(b6, s3, ApplicationStatus.DISBURSED, 87.5, LocalDate.of(2026, 4, 10), "Stage 1 released. Stage 2 progress milestone is OVERDUE (due date was 2026-06-30). Follow-up required!");
        seedOverdueAppScheduleAndMilestone(app17, plan3, new BigDecimal("250000.00"));

        // Edge Case 18: COMPLETED (Fully released across March, May, July, August 2026)
        Application app18 = seedApp(b1, s1, ApplicationStatus.COMPLETED, 96.0, LocalDate.of(2026, 3, 1), "All 4 tranches disbursed. Grid sync inspection certified. Subsidy closed.");
        seedCompletedSchedules(app18, plan1, new BigDecimal("175000.00"),
                LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 25), LocalDate.of(2026, 8, 15));

        // Edge Case 19: COMPLETED (MSME Grant, fully released in April, June, July 2026)
        Application app19 = seedApp(b3, s2, ApplicationStatus.COMPLETED, 93.0, LocalDate.of(2026, 3, 15), "MSME CNC automation grant disbursed in 3 phases. Production verified.");
        seedCompletedSchedules(app19, plan2, new BigDecimal("350000.00"),
                LocalDate.of(2026, 4, 20), LocalDate.of(2026, 6, 10), LocalDate.of(2026, 7, 28), null);

        // Edge Case 20: COMPLETED (Housing Grant, fully released in March, May, July 2026)
        Application app20 = seedApp(b2, s3, ApplicationStatus.COMPLETED, 97.5, LocalDate.of(2026, 2, 20), "Rural permanent home construction finished. Occupancy certificate issued. Fully disbursed.");
        seedCompletedSchedules(app20, plan3, new BigDecimal("250000.00"),
                LocalDate.of(2026, 3, 25), LocalDate.of(2026, 5, 12), LocalDate.of(2026, 7, 15), null);

        // Edge Case 21: APPLICATION_CANCELLED
        Application app21 = seedApp(b8, s4, ApplicationStatus.APPLICATION_CANCELLED, 70.0, LocalDate.of(2026, 5, 5), "Voluntarily cancelled by applicant due to relocation to another district.");

        // 9. Turnaround Verifications (Ensures Approval Turnaround metric computes real values!)
        seedVerification(app18, faUser, VerificationLevel.FINANCE, VerificationDecision.APPROVED, "Final finance approval", LocalDateTime.of(2026, 3, 20, 11, 30));
        seedVerification(app19, faUser, VerificationLevel.FINANCE, VerificationDecision.APPROVED, "Finance sign-off on capital grant", LocalDateTime.of(2026, 4, 5, 14, 0));
        seedVerification(app20, faUser, VerificationLevel.FINANCE, VerificationDecision.APPROVED, "Housing treasury sanction release", LocalDateTime.of(2026, 3, 15, 10, 15));

        // Field-level verifications for Ground Reports tab (ensures Field Officer reports show a verification date)
        seedVerification(app7, foUser, VerificationLevel.FIELD, VerificationDecision.APPROVED, "On-site ground inspection completed. Soil and water depth suitable for solar pump installation.", LocalDateTime.of(2026, 8, 2, 10, 30));
        seedVerification(app9, foUser, VerificationLevel.FIELD, VerificationDecision.APPROVED, "Field verification approved. All documents and site conditions verified.", LocalDateTime.of(2026, 7, 20, 14, 15));
        seedVerification(app10, foUser, VerificationLevel.FIELD, VerificationDecision.APPROVED, "Rural housing site verified. Foundation area and land title confirmed.", LocalDateTime.of(2026, 7, 5, 9, 45));

        // 10. Officer Registration Requests (Ensures Admin 'Officer Requests' tab has live data)
        seedOfficerRequest("Dr. Sneha Kulkarni", "sneha.kulkarni@gov.in", Role.FIELD_OFFICER, "Maharashtra", "9822011223", RequestStatus.PENDING, null, null);
        seedOfficerRequest("Amitabha Roy", "amitabha.roy@gov.in", Role.DISTRICT_OFFICER, "Uttar Pradesh", "9833011224", RequestStatus.PENDING, null, null);
        seedOfficerRequest("Suresh Iyer", "fa@gmail.com", Role.FINANCE_APPROVER, "All Regions", "9844011225", RequestStatus.APPROVED, LocalDateTime.of(2026, 8, 1, 10, 0), adminUser);
        seedOfficerRequest("Rohit Mehra", "rohit.mehra@nic.in", Role.FIELD_OFFICER, "Gujarat", "9855011226", RequestStatus.REJECTED, LocalDateTime.of(2026, 8, 15, 16, 30), adminUser);

        // 11. Audit Logs for Important System Decisions (Ensures Audit Trail tab has live history)
        seedAudit("Scheme", s1.getId(), "SCHEME_CREATED", adminUser, LocalDateTime.of(2026, 1, 10, 9, 30), "Configured PM Surya Ghar scheme with 5 category slabs and 4-stage disbursement plan.");
        seedAudit("Scheme", s2.getId(), "SCHEME_CREATED", adminUser, LocalDateTime.of(2026, 1, 12, 11, 0), "Configured MSME Technology Modernization Grant.");
        seedAudit("Application", app18.getId(), "DISBURSEMENT_COMPLETED", faUser, LocalDateTime.of(2026, 8, 15, 15, 45), "Final tranche of ₹17,500 released. Application #18 marked COMPLETED.");
        seedAudit("Application", app17.getId(), "MILESTONE_OVERDUE", adminUser, LocalDateTime.of(2026, 7, 1, 0, 1), "Compliance milestone #2 flagged OVERDUE by scheduled audit monitor.");
        seedAudit("OfficerRegistrationRequest", 4L, "REQUEST_REJECTED", adminUser, LocalDateTime.of(2026, 8, 15, 16, 30), "Rejected officer registration request for Rohit Mehra (invalid department document).");

        log.info("========== DYNAMIC DATABASE SEEDING COMPLETED SUCCESSFULLY ==========");
    }

    public void truncateDatabase() {
        log.info("Truncating all database tables for clean test seed...");
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(
                "TRUNCATE TABLE audit_logs, verifications, documents, disbursement_milestones, " +
                "application_disbursement_schedules, disbursement_stages, disbursement_plans, " +
                "applications, regional_budgets, scheme_slabs, schemes, beneficiaries, " +
                "officer_registration_requests, users CASCADE"
            );
            log.info("All PostgreSQL tables truncated successfully.");
        } catch (Exception e) {
            log.error("Truncate query error: {}", e.getMessage(), e);
        }
        resetSequences();
    }

    public void resetSequences() {
        log.info("Resetting PostgreSQL sequences to start clean from 1...");
        String[] tables = {
            "users", "beneficiaries", "schemes", "scheme_slabs", "regional_budgets",
            "applications", "disbursement_plans", "disbursement_stages",
            "application_disbursement_schedules", "disbursement_milestones",
            "documents", "verifications", "audit_logs", "officer_registration_requests"
        };
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                try {
                    stmt.execute("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), 1, false)");
                } catch (Exception e) {
                    log.debug("Sequence reset note for table '{}': {}", table, e.getMessage());
                }
            }
            log.info("All PostgreSQL sequences reset successfully.");
        } catch (Exception e) {
            log.warn("Sequence reset error: {}", e.getMessage());
        }
    }

    private User seedUser(String email, String fullName, Role role, String region) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setRegion(region);
        return userRepository.save(user);
    }

    private Beneficiary seedBeneficiary(User user, String name, String aadhaar, String phone,
                                        String address, BeneficiaryCategory category, String region, BigDecimal income) {
        Beneficiary b = new Beneficiary();
        b.setFullName(name);
        b.setNationalIdNumber(aadhaar);
        b.setPhoneNumber(phone);
        b.setAddress(address);
        b.setCategory(category);
        b.setRegistrationDate(LocalDate.now().minusMonths(6));
        b.setRegion(region);
        b.setAnnualIncome(income);
        b.setUser(user);
        return beneficiaryRepository.save(b);
    }

    private Scheme seedScheme(String name, String desc, BigDecimal minIncome, BigDecimal maxIncome,
                              String categories, String documents) {
        Scheme s = new Scheme();
        s.setName(name);
        s.setDescription(desc);
        s.setMinIncome(minIncome);
        s.setMaxIncome(maxIncome);
        s.setAllowedCategories(categories);
        s.setRequiredDocuments(documents);
        s.setActive(true);
        return schemeRepository.save(s);
    }

    private void seedSlabsForScheme(Scheme scheme, BigDecimal gen, BigDecimal obc, BigDecimal sc, BigDecimal st, BigDecimal ews) {
        saveSlab(scheme, BeneficiaryCategory.GENERAL, gen);
        saveSlab(scheme, BeneficiaryCategory.OBC, obc);
        saveSlab(scheme, BeneficiaryCategory.SC, sc);
        saveSlab(scheme, BeneficiaryCategory.ST, st);
        saveSlab(scheme, BeneficiaryCategory.EWS, ews);
    }

    private void saveSlab(Scheme scheme, BeneficiaryCategory cat, BigDecimal amount) {
        SchemeSlab slab = new SchemeSlab();
        slab.setScheme(scheme);
        slab.setCategory(cat);
        slab.setGrantAmount(amount);
        schemeSlabRepository.save(slab);
    }

    private void seedRegionalBudget(Scheme scheme, String region, BigDecimal allocated, BigDecimal utilized) {
        RegionalBudget rb = new RegionalBudget();
        rb.setScheme(scheme);
        rb.setRegionName(region);
        rb.setAllocatedBudget(allocated);
        rb.setUtilizedBudget(utilized);
        regionalBudgetRepository.save(rb);
    }

    private static class StageDef {
        String name;
        int seq;
        int pct;
        TriggerMilestone milestone;
        int offset;
        StageDef(String name, int seq, int pct, TriggerMilestone milestone, int offset) {
            this.name = name;
            this.seq = seq;
            this.pct = pct;
            this.milestone = milestone;
            this.offset = offset;
        }
    }

    private DisbursementPlan seedPlan(Scheme scheme, User admin, int stageCount, StageDef... stages) {
        DisbursementPlan plan = new DisbursementPlan();
        plan.setScheme(scheme);
        plan.setNumberOfStages(stageCount);
        plan.setCreatedBy(admin);
        plan.setCreatedAt(LocalDateTime.now().minusMonths(6));
        DisbursementPlan savedPlan = disbursementPlanRepository.save(plan);

        for (StageDef sd : stages) {
            DisbursementStage s = new DisbursementStage();
            s.setPlan(savedPlan);
            s.setStageName(sd.name);
            s.setSequenceNumber(sd.seq);
            s.setPercentageOfGrant(new BigDecimal(sd.pct + ".00"));
            s.setTriggerMilestone(sd.milestone);
            s.setDueDateOffsetDays(sd.offset);
            disbursementStageRepository.save(s);
        }
        return savedPlan;
    }

    private Application seedApp(Beneficiary b, Scheme s, ApplicationStatus status, double score, LocalDate date, String remarks) {
        Application a = new Application();
        a.setBeneficiary(b);
        a.setScheme(s);
        a.setStatus(status);
        a.setEligibilityScore(score);
        a.setSubmissionDate(date);
        a.setRemarks(remarks);
        return applicationRepository.save(a);
    }

    private void seedAppSchedules(Application app, DisbursementPlan plan, BigDecimal totalGrant, boolean releaseFirstStage, boolean overdue) {
        var stages = disbursementStageRepository.findByPlanIdOrderBySequenceNumberAsc(plan.getId());
        for (DisbursementStage stage : stages) {
            BigDecimal amount = totalGrant.multiply(stage.getPercentageOfGrant()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            ApplicationDisbursementSchedule sch = new ApplicationDisbursementSchedule();
            sch.setApplication(app);
            sch.setStage(stage);
            sch.setScheduledAmount(amount);
            sch.setDueDate(LocalDate.now().plusDays(stage.getDueDateOffsetDays()));

            if (stage.getSequenceNumber() == 1 && releaseFirstStage) {
                sch.setStatus(DisbursementScheduleStatus.RELEASED);
            } else {
                sch.setStatus(DisbursementScheduleStatus.PENDING);
            }
            scheduleRepository.save(sch);

            // Create milestone
            DisbursementMilestone ms = new DisbursementMilestone();
            ms.setApplication(app);
            ms.setStage(stage);
            ms.setMilestoneType(stage.getSequenceNumber() == 1 ? MilestoneType.DOCUMENTATION : MilestoneType.GROUND_VERIFICATION);
            ms.setSequenceOrder(stage.getSequenceNumber());
            ms.setDescription(stage.getStageName());
            ms.setScheduledAmount(amount);
            ms.setDueDate(LocalDate.now().plusDays(stage.getDueDateOffsetDays()));

            if (stage.getSequenceNumber() == 1 && releaseFirstStage) {
                ms.setComplianceStatus(ComplianceStatus.COMPLETED);
                ms.setDisbursementStatus(DisbursementStatus.RELEASED);
                ms.setActualDisbursedDate(LocalDate.now().minusDays(15));
            } else {
                ms.setComplianceStatus(ComplianceStatus.PENDING);
                ms.setDisbursementStatus(DisbursementStatus.NOT_RELEASED);
            }
            milestoneRepository.save(ms);
        }
    }

    private void seedOverdueAppScheduleAndMilestone(Application app, DisbursementPlan plan, BigDecimal totalGrant) {
        var stages = disbursementStageRepository.findByPlanIdOrderBySequenceNumberAsc(plan.getId());
        for (DisbursementStage stage : stages) {
            BigDecimal amount = totalGrant.multiply(stage.getPercentageOfGrant()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            ApplicationDisbursementSchedule sch = new ApplicationDisbursementSchedule();
            sch.setApplication(app);
            sch.setStage(stage);
            sch.setScheduledAmount(amount);

            DisbursementMilestone ms = new DisbursementMilestone();
            ms.setApplication(app);
            ms.setStage(stage);
            ms.setSequenceOrder(stage.getSequenceNumber());
            ms.setDescription(stage.getStageName());
            ms.setScheduledAmount(amount);

            if (stage.getSequenceNumber() == 1) {
                sch.setStatus(DisbursementScheduleStatus.RELEASED);
                sch.setDueDate(LocalDate.of(2026, 4, 15));

                ms.setMilestoneType(MilestoneType.DOCUMENTATION);
                ms.setDueDate(LocalDate.of(2026, 4, 15));
                ms.setComplianceStatus(ComplianceStatus.COMPLETED);
                ms.setDisbursementStatus(DisbursementStatus.RELEASED);
                ms.setActualDisbursedDate(LocalDate.of(2026, 4, 15));
            } else if (stage.getSequenceNumber() == 2) {
                // OVERDUE!
                sch.setStatus(DisbursementScheduleStatus.PENDING);
                sch.setDueDate(LocalDate.of(2026, 6, 30));

                ms.setMilestoneType(MilestoneType.GROUND_VERIFICATION);
                ms.setDueDate(LocalDate.of(2026, 6, 30));
                ms.setComplianceStatus(ComplianceStatus.OVERDUE);
                ms.setDisbursementStatus(DisbursementStatus.NOT_RELEASED);
            } else {
                sch.setStatus(DisbursementScheduleStatus.PENDING);
                sch.setDueDate(LocalDate.of(2026, 9, 30));

                ms.setMilestoneType(MilestoneType.UTILIZATION_PROOF);
                ms.setDueDate(LocalDate.of(2026, 9, 30));
                ms.setComplianceStatus(ComplianceStatus.PENDING);
                ms.setDisbursementStatus(DisbursementStatus.NOT_RELEASED);
            }
            scheduleRepository.save(sch);
            milestoneRepository.save(ms);
        }
    }

    private void seedCompletedSchedules(Application app, DisbursementPlan plan, BigDecimal totalGrant,
                                       LocalDate d1, LocalDate d2, LocalDate d3, LocalDate d4) {
        var stages = disbursementStageRepository.findByPlanIdOrderBySequenceNumberAsc(plan.getId());
        LocalDate[] dates = {d1, d2, d3, d4};

        for (int i = 0; i < stages.size(); i++) {
            DisbursementStage stage = stages.get(i);
            LocalDate date = (i < dates.length && dates[i] != null) ? dates[i] : LocalDate.now().minusDays(30);
            BigDecimal amount = totalGrant.multiply(stage.getPercentageOfGrant()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

            ApplicationDisbursementSchedule sch = new ApplicationDisbursementSchedule();
            sch.setApplication(app);
            sch.setStage(stage);
            sch.setScheduledAmount(amount);
            sch.setDueDate(date);
            sch.setStatus(DisbursementScheduleStatus.RELEASED);
            scheduleRepository.save(sch);

            DisbursementMilestone ms = new DisbursementMilestone();
            ms.setApplication(app);
            ms.setStage(stage);
            ms.setMilestoneType(i == 0 ? MilestoneType.DOCUMENTATION : i == 1 ? MilestoneType.GROUND_VERIFICATION : MilestoneType.UTILIZATION_PROOF);
            ms.setSequenceOrder(stage.getSequenceNumber());
            ms.setDescription(stage.getStageName() + " - Completed");
            ms.setScheduledAmount(amount);
            ms.setDueDate(date);
            ms.setComplianceStatus(ComplianceStatus.COMPLETED);
            ms.setDisbursementStatus(DisbursementStatus.RELEASED);
            ms.setActualDisbursedDate(date);
            milestoneRepository.save(ms);
        }
    }

    private void seedVerification(Application app, User officer, VerificationLevel level, VerificationDecision decision, String remarks, LocalDateTime dt) {
        Verification v = new Verification();
        v.setApplication(app);
        v.setOfficer(officer);
        v.setLevel(level);
        v.setDecision(decision);
        v.setRemarks(remarks);
        v.setVerificationDate(dt);
        verificationRepository.save(v);
    }

    private void seedOfficerRequest(String name, String email, Role role, String region, String phone,
                                    RequestStatus status, LocalDateTime reviewedAt, User reviewedBy) {
        OfficerRegistrationRequest r = new OfficerRegistrationRequest();
        r.setFullName(name);
        r.setEmail(email);
        r.setPasswordHash(passwordEncoder.encode("123456"));
        r.setPhone(phone);
        r.setRequestedRole(role);
        r.setRegion(region);
        r.setStatus(status);
        r.setSubmittedAt(LocalDateTime.now().minusDays(5));
        r.setReviewedAt(reviewedAt);
        r.setReviewedBy(reviewedBy);
        if (status == RequestStatus.REJECTED) {
            r.setRejectionReason("Incomplete employment verification letter");
        }
        officerRegistrationRequestRepository.save(r);
    }

    private void seedAudit(String entityName, Long entityId, String action, User actor, LocalDateTime ts, String details) {
        AuditLog al = new AuditLog();
        al.setEntityName(entityName);
        al.setEntityId(entityId);
        al.setAction(action);
        al.setActor(actor);
        al.setTimestamp(ts);
        al.setDetails(details);
        auditLogRepository.save(al);
    }
}
