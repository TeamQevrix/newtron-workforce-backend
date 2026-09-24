package com.newtron.newtron_workforce_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkOrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    public void testWorkOrderEndpoints() throws Exception {
        // Find Company 9 and its owner
        Company company = companyRepository.findById(9L).orElseThrow();
        User recruiter = company.getOwner();
            
        System.out.println("Using recruiter mobile: " + recruiter.getMobile());
        
        // GET LIST
        String listResponse = mockMvc.perform(get("/api/v1/recruiter/work-orders")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(recruiter.getMobile()).roles("RECRUITER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
                
        System.out.println("LIST RESPONSE: " + listResponse);
        
        // GET DETAIL 1
        String detailResponse = mockMvc.perform(get("/api/v1/recruiter/work-orders/1")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(recruiter.getMobile()).roles("RECRUITER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
                
        System.out.println("DETAIL RESPONSE: " + detailResponse);

        // Security Test - try to fetch a work order that doesn't belong to them if it exists
        System.out.println("SECURITY TEST (expect 4xx):");
        mockMvc.perform(get("/api/v1/recruiter/work-orders/999")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(recruiter.getMobile()).roles("RECRUITER")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is4xxClientError());
    }
}
