package com.subsidytracker.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    // ==========================================
    // 1. PUBLIC ENDPOINTS
    // ==========================================

    @Test
    public void testLogin_Public_ShouldNotBeForbiddenOrUnauthorized() throws Exception {
        // Without authentication, this permitAll endpoint should not return 401 or 403.
        // It might return 400 or 415 because the body/content type is invalid, but not 401 or 403.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    @Test
    public void testRegister_Public_ShouldNotBeForbiddenOrUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    // ==========================================
    // 2. BENEFICIARY SELF-SERVICE
    // ==========================================

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testCreateBeneficiary_Beneficiary_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(post("/api/v1/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "FIELD_OFFICER")
    public void testCreateBeneficiary_Officer_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/beneficiaries")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testGetMyProfile_Beneficiary_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/beneficiaries/me"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testCreateApplication_Beneficiary_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(not(403)));
    }

    // ==========================================
    // 3. OFFICER/ADMIN OVERSIGHT
    // ==========================================

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testGetAllBeneficiaries_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/beneficiaries"))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "FIELD_OFFICER")
    public void testGetAllBeneficiaries_Officer_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/beneficiaries"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testGetAllApplications_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "FIELD_OFFICER")
    public void testGetAllApplications_Officer_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testGetApplicationsByStatus_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/applications/status/SUBMITTED"))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "DISTRICT_OFFICER")
    public void testGetApplicationsByStatus_Officer_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/applications/status/SUBMITTED"))
                .andExpect(status().is(not(403)));
    }

    // ==========================================
    // 4. VERIFICATION
    // ==========================================

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testVerifyApplication_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/applications/1/verify")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "FIELD_OFFICER")
    public void testVerifyApplication_Officer_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/applications/1/verify")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testVerifyApplication_Admin_ShouldBeForbidden() throws Exception {
        // ADMIN role is not configured for verify mapping
        mockMvc.perform(patch("/api/v1/applications/1/verify")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "FIELD_OFFICER")
    public void testVerifyDocument_Officer_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/applications/1/documents/1/verify")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testVerifyDocument_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/applications/1/documents/1/verify")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "DISTRICT_OFFICER")
    public void testResumeVerification_Officer_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/applications/1/resume-verification"))
                .andExpect(status().is(not(403)));
    }

    // ==========================================
    // 5. ADMIN-ONLY OPERATIONS
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCalculateEligibility_Admin_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(post("/api/v1/applications/1/calculate-eligibility"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testCalculateEligibility_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/applications/1/calculate-eligibility"))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "FIELD_OFFICER")
    public void testCreateScheme_Officer_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/schemes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
    }

    // ==========================================
    // 6. FINANCE_APPROVER-SPECIFIC OPERATION
    // ==========================================

    @Test
    @WithMockUser(roles = "FINANCE_APPROVER")
    public void testGenerateSchedule_FinanceApprover_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(post("/api/disbursement/schedules/generate/1"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGenerateSchedule_Admin_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(post("/api/disbursement/schedules/generate/1"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testGenerateSchedule_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/disbursement/schedules/generate/1"))
                .andExpect(status().is(403));
    }

    // ==========================================
    // 7. COMPLIANCE OPERATIONS
    // ==========================================

    @Test
    @WithMockUser(roles = "FIELD_OFFICER")
    public void testCompleteComplianceMilestone_Officer_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(put("/api/disbursement/compliance/1/complete"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testCompleteComplianceMilestone_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(put("/api/disbursement/compliance/1/complete"))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCompleteComplianceMilestone_Admin_ShouldBeForbidden() throws Exception {
        mockMvc.perform(put("/api/disbursement/compliance/1/complete"))
                .andExpect(status().is(403));
    }

    // ==========================================
    // 8. ANALYTICS / DASHBOARD / REPORTS
    // ==========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAnalyticsOverview_Admin_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "BENEFICIARY")
    public void testAnalyticsOverview_Beneficiary_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().is(403));
    }

    @Test
    @WithMockUser(roles = "DISTRICT_OFFICER")
    public void testDashboardOverview_Officer_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @WithMockUser(roles = "FINANCE_APPROVER")
    public void testReportsSchemesPdf_Approver_ShouldBeAuthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/schemes/pdf"))
                .andExpect(status().is(not(403)));
    }

    // ==========================================
    // 9. UNAUTHENTICATED ACCESS
    // ==========================================

    @Test
    public void testUnauthenticatedAccess_Applications_ShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().is(anyOf(is(401), is(403))));
    }

    @Test
    public void testUnauthenticatedAccess_Schemes_ShouldBeUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/schemes"))
                .andExpect(status().is(anyOf(is(401), is(403))));
    }

    @Test
    public void testUnauthenticatedAccess_Plans_ShouldBeUnauthorized() throws Exception {
        mockMvc.perform(post("/api/disbursement/plans"))
                .andExpect(status().is(anyOf(is(401), is(403))));
    }
}
