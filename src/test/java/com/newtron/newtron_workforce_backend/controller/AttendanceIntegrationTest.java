package com.newtron.newtron_workforce_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.dto.AttendanceRequestDto;
import com.newtron.newtron_workforce_backend.entity.*;
import com.newtron.newtron_workforce_backend.enums.AgreementStatus;
import com.newtron.newtron_workforce_backend.enums.AttendanceStatus;
import com.newtron.newtron_workforce_backend.enums.WorkOrderStatus;
import com.newtron.newtron_workforce_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AttendanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

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

    @MockBean
    private com.newtron.newtron_workforce_backend.auth.otp.provider.OtpProvider otpProvider;

    private User recruiter;
    private User worker;
    private Company company;
    private Job job;
    private Application application;
    private Agreement agreement;
    private WorkOrder workOrder;

    @BeforeEach
    void setUp() {
        recruiter = User.builder()
                .mobile("9999999991")
                .fullName("Recruiter Test")
                .role(com.newtron.newtron_workforce_backend.auth.enums.Role.RECRUITER)
                .build();
        recruiter = userRepository.save(recruiter);

        worker = User.builder()
                .mobile("8888888881")
                .fullName("Worker Test")
                .role(com.newtron.newtron_workforce_backend.auth.enums.Role.WORKER)
                .build();
        worker = userRepository.save(worker);

        company = Company.builder()
                .companyName("Test Company")
                .contactPersonName("John Doe")
                .contactMobile("9999999991")
                .city("Test City")
                .owner(recruiter)
                .profileCompleted(true)
                .build();
        company = companyRepository.save(company);

        job = Job.builder()
                .title("Test Job")
                .recruiter(recruiter)
                .status("Active")
                .engagementType("DAILY")
                .build();
        job = jobRepository.save(job);

        application = Application.builder()
                .job(job)
                .worker(worker)
                .status("Shortlisted")
                .build();
        application = applicationRepository.save(application);

        agreement = Agreement.builder()
                .application(application)
                .worker(worker)
                .company(company)
                .job(job)
                .status(AgreementStatus.ACTIVE)
                .clientAcceptedAt(Instant.now())
                .workerAcceptedAt(Instant.now())
                .engagementType("DAILY")
                .commissionRate(BigDecimal.valueOf(0.05))
                .build();
        agreement = agreementRepository.save(agreement);

        workOrder = WorkOrder.builder()
                .workOrderNumber("WO-TEST-001")
                .agreement(agreement)
                .company(company)
                .job(job)
                .worker(worker)
                .status(WorkOrderStatus.ACTIVE)
                .engagementType("DAILY")
                .dailyWage(BigDecimal.valueOf(1000))
                .commissionRate(BigDecimal.valueOf(0.05))
                .commissionAmount(BigDecimal.valueOf(50))
                .commissionPayer("WORKER")
                .expectedStartDate(Instant.now())
                .actualStartDate(Instant.now())
                .build();
        workOrder = workOrderRepository.save(workOrder);
    }

    @Test
    @WithMockUser(username = "9999999991")
    void markAttendance_ActiveWorkOrder_Present_Success() throws Exception {
        AttendanceRequestDto request = new AttendanceRequestDto();
        request.setAttendanceDate(LocalDate.now());
        request.setStatus(AttendanceStatus.PRESENT);
        request.setRemark("On time");

        mockMvc.perform(post("/api/v1/recruiter/work-orders/" + workOrder.getId() + "/attendance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PRESENT"));
    }

    @Test
    @WithMockUser(username = "9999999991")
    void markAttendance_DuplicateDate_Rejects() throws Exception {
        Attendance attendance = Attendance.builder()
                .workOrder(workOrder)
                .attendanceDate(LocalDate.now())
                .status(AttendanceStatus.ABSENT)
                .markedBy(recruiter)
                .build();
        attendanceRepository.save(attendance);

        AttendanceRequestDto request = new AttendanceRequestDto();
        request.setAttendanceDate(LocalDate.now());
        request.setStatus(AttendanceStatus.PRESENT);

        mockMvc.perform(post("/api/v1/recruiter/work-orders/" + workOrder.getId() + "/attendance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Attendance already exists for this date"));
    }

    @Test
    @WithMockUser(username = "9999999991")
    void markAttendance_CompletedWorkOrder_Rejects() throws Exception {
        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        workOrderRepository.save(workOrder);

        AttendanceRequestDto request = new AttendanceRequestDto();
        request.setAttendanceDate(LocalDate.now());
        request.setStatus(AttendanceStatus.PRESENT);

        mockMvc.perform(post("/api/v1/recruiter/work-orders/" + workOrder.getId() + "/attendance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Work order must be ACTIVE to mark attendance"));
    }

    @Test
    @WithMockUser(username = "9999999991")
    void fetchAttendance_ReturnsRecords() throws Exception {
        Attendance attendance1 = Attendance.builder()
                .workOrder(workOrder)
                .attendanceDate(LocalDate.now().minusDays(1))
                .status(AttendanceStatus.PRESENT)
                .markedBy(recruiter)
                .build();
        attendanceRepository.save(attendance1);

        Attendance attendance2 = Attendance.builder()
                .workOrder(workOrder)
                .attendanceDate(LocalDate.now())
                .status(AttendanceStatus.HALF_DAY)
                .markedBy(recruiter)
                .build();
        attendanceRepository.save(attendance2);

        mockMvc.perform(get("/api/v1/recruiter/work-orders/" + workOrder.getId() + "/attendance")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].status").value("HALF_DAY"))
                .andExpect(jsonPath("$.data[1].status").value("PRESENT"));
    }

    @Test
    @WithMockUser(username = "9999999991")
    void markAttendance_MissingDate_Rejects() throws Exception {
        AttendanceRequestDto request = new AttendanceRequestDto();
        request.setStatus(AttendanceStatus.PRESENT);

        mockMvc.perform(post("/api/v1/recruiter/work-orders/" + workOrder.getId() + "/attendance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
