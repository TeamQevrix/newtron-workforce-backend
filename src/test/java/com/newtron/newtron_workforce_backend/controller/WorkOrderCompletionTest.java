package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.dto.WorkOrderCompleteRequestDto;
import com.newtron.newtron_workforce_backend.entity.Agreement;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.enums.AgreementStatus;
import com.newtron.newtron_workforce_backend.enums.WorkOrderStatus;
import com.newtron.newtron_workforce_backend.repository.AgreementRepository;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class WorkOrderCompletionTest {

    @Autowired
    private RecruiterWorkOrderController controller;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private com.newtron.newtron_workforce_backend.auth.repository.UserRepository userRepository;

    @Test
    public void testCompletionLogic() {
        System.out.println("--- 1. BEFORE STATE ---");
        Optional<WorkOrder> woOpt = workOrderRepository.findById(1L);
        if (woOpt.isEmpty()) {
            System.out.println("WO-1 not found. Please create one.");
            return;
        }
        WorkOrder wo = woOpt.get();
        System.out.println("WorkOrder ID: " + wo.getId());
        System.out.println("WorkOrder number: " + wo.getWorkOrderNumber());
        System.out.println("WorkOrder status: " + wo.getStatus());
        System.out.println("actualEndDate: " + wo.getActualEndDate());
        System.out.println("Agreement ID: " + (wo.getAgreement() != null ? wo.getAgreement().getId() : "null"));
        Agreement agg = agreementRepository.findById(wo.getAgreement().getId()).orElse(null);
        System.out.println("Agreement status: " + (agg != null ? agg.getStatus() : "null"));
        
        if (wo.getStatus() == WorkOrderStatus.COMPLETED) {
            System.out.println("WO-1 is already COMPLETED. Stop testing.");
            return;
        }

        System.out.println("--- 2. EXECUTING ENDPOINT ---");
        MockHttpServletRequest request = new MockHttpServletRequest();
        User owner = userRepository.findById(wo.getCompany().getOwner().getId()).get();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(owner.getMobile())
                .password("password")
                .authorities("ROLE_RECRUITER")
                .build();
                
        request.setAttribute("startTimeMs", System.currentTimeMillis());

        Instant now = Instant.now();
        WorkOrderCompleteRequestDto req = new WorkOrderCompleteRequestDto(now);
        
        ApiResponse<Map<String, Object>> response = controller.completeWorkOrder(1L, req, userDetails, request);
        System.out.println("HTTP Response Status: " + response.isSuccess());
        
        System.out.println("--- 3. AFTER STATE (DB QUERY) ---");
        WorkOrder woAfter = workOrderRepository.findById(1L).get();
        Agreement aggAfter = agreementRepository.findById(woAfter.getAgreement().getId()).get();
        System.out.println("WorkOrder status: " + woAfter.getStatus());
        System.out.println("actualEndDate: " + woAfter.getActualEndDate());
        System.out.println("Agreement status: " + aggAfter.getStatus());

        System.out.println("--- 4. REPEAT COMPLETION ---");
        try {
            controller.completeWorkOrder(1L, req, userDetails, request);
            System.out.println("ERROR: Expected exception, but got success!");
        } catch (ValidationException e) {
            System.out.println("Caught expected ValidationException: " + e.getMessage());
        }
        
        System.out.println("--- 5. VERIFYING FIELD TYPES ---");
        System.out.println("actualEndDate Java Type: " + woAfter.getActualEndDate().getClass().getName());
        System.out.println("actualEndDate Value: " + woAfter.getActualEndDate().toString());

        System.out.println("ALL TESTS FINISHED.");
    }
}
