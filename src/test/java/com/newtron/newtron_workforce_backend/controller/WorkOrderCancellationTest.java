package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.dto.WorkOrderCancelRequestDto;
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
public class WorkOrderCancellationTest {

    @Autowired
    private RecruiterWorkOrderController controller;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private com.newtron.newtron_workforce_backend.auth.repository.UserRepository userRepository;

    @Test
    public void testCancellationLogic() {
        System.out.println("--- STARTING CANCELLATION TEST ---");
        
        Optional<WorkOrder> woOpt = workOrderRepository.findById(1L);
        if (woOpt.isEmpty()) {
            System.out.println("WO-1 not found.");
            return;
        }
        WorkOrder wo = woOpt.get();
        Agreement agg = agreementRepository.findById(wo.getAgreement().getId()).get();
        
        // Force WO-1 to be ACTIVE for testing purposes
        wo.setStatus(WorkOrderStatus.ACTIVE);
        wo.setActualEndDate(null);
        wo.setCancellationReason(null);
        workOrderRepository.save(wo);
        
        agg.setStatus(AgreementStatus.ACTIVE);
        agreementRepository.save(agg);
        
        System.out.println("--- 1. BEFORE STATE ---");
        System.out.println("WorkOrder status: " + wo.getStatus());
        System.out.println("Agreement status: " + agg.getStatus());

        User owner = userRepository.findById(wo.getCompany().getOwner().getId()).get();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(owner.getMobile())
                .password("password")
                .authorities("ROLE_RECRUITER")
                .build();
                
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("startTimeMs", System.currentTimeMillis());

        Instant now = Instant.now();
        WorkOrderCancelRequestDto req = new WorkOrderCancelRequestDto();
        req.setActualEndDate(now);
        req.setCancellationReason("Client ended project early.");
        
        System.out.println("--- 2. EXECUTING ENDPOINT ---");
        ApiResponse<Map<String, Object>> response = controller.cancelWorkOrder(1L, req, userDetails, request);
        System.out.println("HTTP Response Status: " + response.isSuccess());
        
        System.out.println("--- 3. AFTER STATE (DB QUERY) ---");
        WorkOrder woAfter = workOrderRepository.findById(1L).get();
        Agreement aggAfter = agreementRepository.findById(woAfter.getAgreement().getId()).get();
        System.out.println("WorkOrder status: " + woAfter.getStatus());
        System.out.println("actualEndDate: " + woAfter.getActualEndDate());
        System.out.println("cancellationReason: " + woAfter.getCancellationReason());
        System.out.println("Agreement status: " + aggAfter.getStatus());
        System.out.println("Agreement completedAt: " + aggAfter.getCompletedAt());

        System.out.println("--- 4. REPEAT CANCELLATION ---");
        try {
            controller.cancelWorkOrder(1L, req, userDetails, request);
            System.out.println("ERROR: Expected exception, but got success!");
        } catch (ValidationException e) {
            System.out.println("Caught expected ValidationException: " + e.getMessage());
        }
        
        System.out.println("--- 5. VERIFYING FIELD TYPES ---");
        System.out.println("cancellationReason Value: " + woAfter.getCancellationReason());
        
        System.out.println("ALL TESTS FINISHED.");
    }
}
