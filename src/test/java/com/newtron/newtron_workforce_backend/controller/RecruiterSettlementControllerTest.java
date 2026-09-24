package com.newtron.newtron_workforce_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.dto.CalculateSettlementRequestDto;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.entity.Settlement;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.SettlementRepository;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RecruiterSettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SettlementService settlementService;

    @MockBean
    private SettlementRepository settlementRepository;

    @MockBean
    private WorkOrderRepository workOrderRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CompanyRepository companyRepository;

    @MockBean
    private WorkerProfileRepository workerProfileRepository;

    @MockBean
    private com.newtron.newtron_workforce_backend.auth.otp.provider.OtpProvider otpProvider;

    private User authUser;
    private Company authCompany;
    private WorkOrder validWorkOrder;
    private Settlement mockSettlement;
    private WorkerProfile mockWorkerProfile;
    private User workerUser;

    @BeforeEach
    void setUp() {
        authUser = new User();
        authUser.setId(10L);
        authUser.setMobile("+919876543210");

        authCompany = new Company();
        authCompany.setId(100L);
        authCompany.setOwner(authUser);

        workerUser = new User();
        workerUser.setId(20L);

        validWorkOrder = new WorkOrder();
        validWorkOrder.setId(1L);
        validWorkOrder.setWorkOrderNumber("WO-123");
        validWorkOrder.setCompany(authCompany);
        validWorkOrder.setWorker(workerUser);

        mockWorkerProfile = new WorkerProfile();
        mockWorkerProfile.setId(99L);
        mockWorkerProfile.setFullName("Test Worker");
        mockWorkerProfile.setUser(workerUser);

        mockSettlement = new Settlement();
        mockSettlement.setId(1000L);
        mockSettlement.setWorkOrder(validWorkOrder);
        mockSettlement.setSettlementMonth(8);
        mockSettlement.setSettlementYear(2026);
        mockSettlement.setWorkerPayableAmount(new BigDecimal("15000.00"));
        mockSettlement.setNewtronCommissionAmount(new BigDecimal("750.00"));
        mockSettlement.setTotalClientPayableAmount(new BigDecimal("15750.00"));

        when(userRepository.findByMobile("+919876543210")).thenReturn(Optional.of(authUser));
        when(companyRepository.findByOwnerId(10L)).thenReturn(Optional.of(authCompany));
    }

    @Test
    @WithMockUser(username = "+919876543210")
    void testCalculateSettlement_Success() throws Exception {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        when(settlementService.calculateAndSaveSettlement(1L, 8, 2026)).thenReturn(mockSettlement);
        when(workerProfileRepository.findByUserId(20L)).thenReturn(Optional.of(mockWorkerProfile));

        CalculateSettlementRequestDto request = new CalculateSettlementRequestDto(1L, 8, 2026);

        mockMvc.perform(post("/api/v1/recruiter/settlements/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workerId").value(99L)) // WorkerProfile.id
                .andExpect(jsonPath("$.data.workerPayableAmount").value(15000.0))
                .andExpect(jsonPath("$.data.newtronCommissionAmount").value(750.0))
                .andExpect(jsonPath("$.data.totalClientPayableAmount").value(15750.0));
    }

    @Test
    @WithMockUser(username = "+919876543210")
    void testCalculateSettlement_CrossCompanyRejected() throws Exception {
        Company otherCompany = new Company();
        otherCompany.setId(200L);
        WorkOrder otherWorkOrder = new WorkOrder();
        otherWorkOrder.setId(2L);
        otherWorkOrder.setCompany(otherCompany);

        when(workOrderRepository.findById(2L)).thenReturn(Optional.of(otherWorkOrder));

        CalculateSettlementRequestDto request = new CalculateSettlementRequestDto(2L, 8, 2026);

        mockMvc.perform(post("/api/v1/recruiter/settlements/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].message").value("WorkOrder not found"));
    }

    @Test
    @WithMockUser(username = "+919876543210")
    void testCalculateSettlement_DuplicateRejected() throws Exception {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        when(settlementService.calculateAndSaveSettlement(1L, 8, 2026))
                .thenThrow(new IllegalStateException("Duplicate settlement rejected for this month and year"));

        CalculateSettlementRequestDto request = new CalculateSettlementRequestDto(1L, 8, 2026);

        mockMvc.perform(post("/api/v1/recruiter/settlements/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].message").value("Duplicate settlement rejected for this month and year"));
    }

    @Test
    @WithMockUser(username = "+919876543210")
    void testListSettlements_Success() throws Exception {
        when(settlementRepository.findByCompanyIdWithFilters(eq(100L), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(mockSettlement)));

        when(workerProfileRepository.findByUserId(20L)).thenReturn(Optional.of(mockWorkerProfile));

        mockMvc.perform(get("/api/v1/recruiter/settlements")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].workerId").value(99L));
    }

    @Test
    @WithMockUser(username = "+919876543210")
    void testGetSettlementDetail_Success() throws Exception {
        when(settlementRepository.findByIdAndCompanyId(1000L, 100L)).thenReturn(Optional.of(mockSettlement));
        when(workerProfileRepository.findByUserId(20L)).thenReturn(Optional.of(mockWorkerProfile));

        mockMvc.perform(get("/api/v1/recruiter/settlements/1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workerId").value(99L));
    }

    @Test
    @WithMockUser(username = "+919876543210")
    void testGetSettlementDetail_CrossCompanyRejected() throws Exception {
        when(settlementRepository.findByIdAndCompanyId(1000L, 100L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/recruiter/settlements/1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].message").value("Settlement not found"));
    }
}
