package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.AttendanceBulkResponseDto;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.WorkOrderStatus;
import com.newtron.newtron_workforce_backend.repository.AttendanceRepository;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceBulkServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private WorkerProfileRepository workerProfileRepository;
    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private AttendanceBulkServiceImpl attendanceBulkService;

    private User currentUser;
    private Company company;
    private WorkOrder workOrder;
    private WorkerProfile workerProfile;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);

        company = new Company();
        company.setId(10L);

        User workerUser = new User();
        workerUser.setId(2L);

        workerProfile = new WorkerProfile();
        workerProfile.setId(90L);
        workerProfile.setUser(workerUser);

        workOrder = new WorkOrder();
        workOrder.setId(30L);
        workOrder.setWorkOrderNumber("WO-30");
        workOrder.setCompany(company);
        workOrder.setStatus(WorkOrderStatus.ACTIVE);
        workOrder.setWorker(workerUser);
    }

    private byte[] createExcel(String[][] data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance");
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < data[i].length; j++) {
                    row.createCell(j).setCellValue(data[i][j]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 1 & 14. Valid multi-row import and Successful importedCount
    @Test
    void testValidMultiRowImport() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-22", "WO-30", "90", "PRESENT", "Good"},
                {"2026-09-23", "WO-30", "90", "ABSENT", "Sick"}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));
        when(attendanceRepository.existsByWorkOrderIdAndAttendanceDate(any(), any())).thenReturn(false);

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNull(response.getErrors());
        assertEquals(2, response.getImportedCount());
        verify(attendanceRepository, times(1)).saveAll(any());
    }

    // 2. Missing Work Order
    @Test
    void testMissingWorkOrder() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-23", "WO-99", "90", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-99")).thenReturn(Optional.empty());
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        assertEquals("Work Order not found.", response.getErrors().get(0).getReason());
        verify(attendanceRepository, never()).saveAll(any());
    }

    // 3. Invalid date
    @Test
    void testInvalidDate() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"invalid-date", "WO-30", "90", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("Invalid date format. Expected YYYY-MM-DD.", response.getErrors().get(0).getReason());
        verify(attendanceRepository, never()).saveAll(any());
    }

    // 4. Future date
    @Test
    void testFutureDate() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {LocalDate.now().plusDays(5).toString(), "WO-30", "90", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("Date cannot be in the future.", response.getErrors().get(0).getReason());
    }

    // 5. Invalid status
    @Test
    void testInvalidStatus() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-23", "WO-30", "90", "UNKNOWN", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("Invalid Status. Must be exactly PRESENT, ABSENT, or HALF_DAY.", response.getErrors().get(0).getReason());
    }

    // 6. Work Order belongs to another company
    @Test
    void testUnauthorizedWorkOrder() throws Exception {
        Company otherCompany = new Company();
        otherCompany.setId(99L); // different from current company 10L
        workOrder.setCompany(otherCompany);

        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-23", "WO-30", "90", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("Work Order does not belong to your company.", response.getErrors().get(0).getReason());
    }

    // 7. COMPLETED Work Order
    @Test
    void testCompletedWorkOrder() throws Exception {
        workOrder.setStatus(WorkOrderStatus.COMPLETED);

        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-23", "WO-30", "90", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("Work Order is not ACTIVE. Current status: COMPLETED", response.getErrors().get(0).getReason());
    }

    // 8. CANCELLED Work Order
    @Test
    void testCancelledWorkOrder() throws Exception {
        workOrder.setStatus(WorkOrderStatus.CANCELLED);

        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-23", "WO-30", "90", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("Work Order is not ACTIVE. Current status: CANCELLED", response.getErrors().get(0).getReason());
    }

    // 9. Worker ID mismatch
    @Test
    void testWorkerIdMismatch() throws Exception {
        User anotherUser = new User();
        anotherUser.setId(99L);
        WorkerProfile anotherWorkerProfile = new WorkerProfile();
        anotherWorkerProfile.setId(91L);
        anotherWorkerProfile.setUser(anotherUser);

        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-23", "WO-30", "91", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder)); // WO expects worker user id 2
        when(workerProfileRepository.findById(91L)).thenReturn(Optional.of(anotherWorkerProfile));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("Worker ID does not match the assigned Worker for this Work Order.", response.getErrors().get(0).getReason());
    }

    // 10. Duplicate Work Order + Date inside the same Excel
    @Test
    void testDuplicateInsideExcel() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-23", "WO-30", "90", "PRESENT", ""},
                {"2026-09-23", "WO-30", "90", "ABSENT", ""} // Duplicate!
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));
        when(attendanceRepository.existsByWorkOrderIdAndAttendanceDate(any(), any())).thenReturn(false);

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        assertEquals("Duplicate Work Order and Date found within the same Excel file.", response.getErrors().get(0).getReason());
    }

    // 11. Existing database attendance duplicate
    @Test
    void testExistingDatabaseDuplicate() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-23", "WO-30", "90", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));
        when(attendanceRepository.existsByWorkOrderIdAndAttendanceDate(30L, LocalDate.parse("2026-09-23"))).thenReturn(true);

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("Attendance already exists for this Work Order on the given date.", response.getErrors().get(0).getReason());
    }

    // 12. Missing required header
    @Test
    void testMissingHeader() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Status", "Remark"}, // Worker ID is missing
                {"2026-09-23", "WO-30", "PRESENT", ""}
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertTrue(response.getErrors().get(0).getReason().contains("Invalid or missing headers"));
    }

    // 13. Empty Excel file
    @Test
    void testEmptyExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals("File is missing or empty.", response.getErrors().get(0).getReason());
    }

    // 15. Atomic failure: valid rows + one invalid row => ZERO Attendance records persisted
    @Test
    void testAtomicFailureDoesNotPersistValidRows() throws Exception {
        String[][] data = {
                {"Date", "Work Order Number", "Worker ID", "Status", "Remark"},
                {"2026-09-22", "WO-30", "90", "PRESENT", "Good"}, // Valid
                {"2026-09-23", "WO-99", "90", "PRESENT", "Good"}  // Invalid WO
        };
        byte[] content = createExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        when(companyRepository.findByOwnerId(1L)).thenReturn(Optional.of(company));
        when(workOrderRepository.findByWorkOrderNumber("WO-30")).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.findByWorkOrderNumber("WO-99")).thenReturn(Optional.empty());
        when(workerProfileRepository.findById(90L)).thenReturn(Optional.of(workerProfile));
        when(attendanceRepository.existsByWorkOrderIdAndAttendanceDate(eq(30L), any())).thenReturn(false);

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        assertEquals("Work Order not found.", response.getErrors().get(0).getReason());
        
        // Assert ZERO records saved
        verify(attendanceRepository, never()).saveAll(any());
    }
}
