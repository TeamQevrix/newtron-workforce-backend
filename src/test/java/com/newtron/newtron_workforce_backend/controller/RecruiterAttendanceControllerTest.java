package com.newtron.newtron_workforce_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.security.jwt.JwtTokenProvider;
import com.newtron.newtron_workforce_backend.security.jwt.JwtPrincipal;
import com.newtron.newtron_workforce_backend.entity.*;
import com.newtron.newtron_workforce_backend.enums.AttendanceStatus;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import com.newtron.newtron_workforce_backend.enums.WorkOrderStatus;
import com.newtron.newtron_workforce_backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RecruiterAttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private WorkerProfileRepository workerProfileRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.newtron.newtron_workforce_backend.auth.otp.provider.OtpProvider otpProvider;

    private User recruiter;
    private String recruiterToken;
    private Company company;
    private WorkOrder workOrder;
    private WorkerProfile workerProfile;

    @BeforeEach
    void setUp() {
        String randomStr1 = UUID.randomUUID().toString().substring(0, 8);
        recruiter = User.builder()
                .fullName("Test Recruiter 1")
                .mobile("+91" + randomStr1)
                .role(Role.RECRUITER)
                .profileCompleted(true)
                .build();
        recruiter = userRepository.save(recruiter);

        company = Company.builder()
                .owner(recruiter)
                .companyName("Test Company 1")
                .contactPersonName("Test Person")
                .contactMobile("+91" + randomStr1)
                .city("Test City")
                .profileCompleted(true)
                .build();
        company = companyRepository.save(company);

        JwtPrincipal principal = JwtPrincipal.builder()
                .userId(recruiter.getId())
                .mobile(recruiter.getMobile())
                .userType(recruiter.getRole().name())
                .authorities(new ArrayList<>())
                .build();
        recruiterToken = jwtTokenProvider.generateAccessToken(principal);

        String randomStr2 = UUID.randomUUID().toString().substring(0, 8);
        User worker = User.builder()
                .fullName("Test Worker")
                .mobile("+91" + randomStr2)
                .role(Role.WORKER)
                .profileCompleted(true)
                .build();
        worker = userRepository.save(worker);

        workerProfile = WorkerProfile.builder()
                .user(worker)
                .fullName("Test Worker")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(com.newtron.newtron_workforce_backend.enums.Gender.MALE)
                .currentStep(OnboardingStep.COMPLETED)
                .isCompleted(true)
                .build();
        workerProfile = workerProfileRepository.save(workerProfile);

        Job job = Job.builder()
                .company(company)
                .title("Test Job")
                .status("Active")
                .build();
        job = jobRepository.save(job);

        Application application = Application.builder()
                .job(job)
                .worker(worker)
                .status("Hired")
                .build();
        application = applicationRepository.save(application);

        Agreement agreement = Agreement.builder()
                .application(application)
                .company(company)
                .job(job)
                .worker(worker)
                .status(com.newtron.newtron_workforce_backend.enums.AgreementStatus.ACTIVE)
                .engagementType("DAILY")
                .dailyWage(BigDecimal.valueOf(500))
                .build();
        agreement = agreementRepository.save(agreement);

        workOrder = WorkOrder.builder()
                .workOrderNumber("WO-TEST-1")
                .agreement(agreement)
                .company(company)
                .job(job)
                .worker(worker)
                .status(WorkOrderStatus.ACTIVE)
                .engagementType("DAILY")
                .dailyWage(BigDecimal.valueOf(500))
                .commissionRate(BigDecimal.ZERO)
                .build();
        workOrder = workOrderRepository.save(workOrder);

        // Attendance 1
        Attendance attendance1 = Attendance.builder()
                .workOrder(workOrder)
                .attendanceDate(LocalDate.of(2026, 9, 23))
                .status(AttendanceStatus.PRESENT)
                .remark("Present today")
                .markedBy(recruiter)
                .build();
        attendance1 = attendanceRepository.save(attendance1);

        // Attendance 2
        Attendance attendance2 = Attendance.builder()
                .workOrder(workOrder)
                .attendanceDate(LocalDate.of(2026, 9, 22))
                .status(AttendanceStatus.ABSENT)
                .remark("Absent yesterday")
                .markedBy(recruiter)
                .build();
        attendance2 = attendanceRepository.save(attendance2);
    }

    @Test
    void shouldReturnCentralAttendance_WithoutDateFilter() throws Exception {
        mockMvc.perform(get("/api/v1/recruiter/attendance")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].attendanceDate").value("2026-09-23"))
                .andExpect(jsonPath("$.data.content[0].status").value("PRESENT"))
                .andExpect(jsonPath("$.data.content[0].workOrderNumber").value("WO-TEST-1"))
                .andExpect(jsonPath("$.data.content[0].workerName").value("Test Worker"))
                .andExpect(jsonPath("$.data.content[0].workerProfileId").value(workerProfile.getId().intValue()))
                .andExpect(jsonPath("$.data.content[1].attendanceDate").value("2026-09-22"))
                .andExpect(jsonPath("$.data.content[1].status").value("ABSENT"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void shouldReturnCentralAttendance_WithDateFilter() throws Exception {
        mockMvc.perform(get("/api/v1/recruiter/attendance?date=2026-09-22")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].attendanceDate").value("2026-09-22"))
                .andExpect(jsonPath("$.data.content[0].status").value("ABSENT"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void shouldReturnEmptyPage_IfNoAttendanceForCompany() throws Exception {
        // Delete existing attendance
        attendanceRepository.deleteAll();

        mockMvc.perform(get("/api/v1/recruiter/attendance")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.empty").value(true));
    }

    @Test
    void shouldNotReturnOtherCompanysAttendance() throws Exception {
        String randomStr3 = UUID.randomUUID().toString().substring(0, 8);
        User recruiter2 = User.builder()
                .fullName("Test Recruiter 2")
                .mobile("+91" + randomStr3)
                .role(Role.RECRUITER)
                .profileCompleted(true)
                .build();
        recruiter2 = userRepository.save(recruiter2);

        Company company2 = Company.builder()
                .owner(recruiter2)
                .companyName("Test Company 2")
                .contactPersonName("Test Person")
                .contactMobile("+91" + randomStr3)
                .city("Test City")
                .profileCompleted(true)
                .build();
        company2 = companyRepository.save(company2);

        JwtPrincipal principal2 = JwtPrincipal.builder()
                .userId(recruiter2.getId())
                .mobile(recruiter2.getMobile())
                .userType(recruiter2.getRole().name())
                .authorities(new ArrayList<>())
                .build();
        String token2 = jwtTokenProvider.generateAccessToken(principal2);

        // Recruiter 2 queries central attendance
        mockMvc.perform(get("/api/v1/recruiter/attendance")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(0))) // Should not see company 1's attendance
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
