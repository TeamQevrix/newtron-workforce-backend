package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.entity.Attendance;
import com.newtron.newtron_workforce_backend.entity.Settlement;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.enums.AttendanceStatus;
import com.newtron.newtron_workforce_backend.repository.AttendanceRepository;
import com.newtron.newtron_workforce_backend.repository.SettlementRepository;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    private WorkOrder validWorkOrder;

    @BeforeEach
    void setUp() {
        validWorkOrder = new WorkOrder();
        validWorkOrder.setId(1L);
        validWorkOrder.setEngagementType("MONTHLY");
        validWorkOrder.setMonthlySalary(new BigDecimal("15000.00"));
        validWorkOrder.setCommissionRate(new BigDecimal("0.05"));
        validWorkOrder.setCommissionAmount(new BigDecimal("750.00"));
    }

    private Attendance createAttendance(int day, AttendanceStatus status) {
        Attendance a = new Attendance();
        a.setAttendanceDate(LocalDate.of(2026, 8, day));
        a.setStatus(status);
        return a;
    }

    @Test
    void testInvalidMonthRejected() {
        assertThrows(IllegalArgumentException.class, () -> settlementService.calculateAndSaveSettlement(1L, 13, 2026));
    }

    @Test
    void testDailyWorkOrderRejected() {
        validWorkOrder.setEngagementType("DAILY");
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        assertThrows(IllegalArgumentException.class, () -> settlementService.calculateAndSaveSettlement(1L, 8, 2026));
    }

    @Test
    void testDuplicateSettlementRejected() {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        when(settlementRepository.findByWorkOrderIdAndSettlementMonthAndSettlementYearAndDeletedFalse(1L, 8, 2026))
                .thenReturn(Optional.of(new Settlement()));
        assertThrows(IllegalStateException.class, () -> settlementService.calculateAndSaveSettlement(1L, 8, 2026));
    }

    @Test
    void testFullAttendance_workerPayableEqualsSalary() {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        List<Attendance> attendances = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            attendances.add(createAttendance(i, AttendanceStatus.PRESENT));
        }
        when(attendanceRepository.findByWorkOrderIdOrderByAttendanceDateDesc(1L)).thenReturn(attendances);
        
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArguments()[0]);

        Settlement settlement = settlementService.calculateAndSaveSettlement(1L, 8, 2026);
        
        // 15000 / 30 = 500 * 30 = 15000
        assertEquals(new BigDecimal("15000.00"), settlement.getWorkerPayableAmount());
        assertEquals(new BigDecimal("750.00"), settlement.getNewtronCommissionAmount());
        assertEquals(new BigDecimal("15750.00"), settlement.getTotalClientPayableAmount());
        
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void testPartialAttendance_Mixed() {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        List<Attendance> attendances = new ArrayList<>();
        for (int i = 1; i <= 20; i++) attendances.add(createAttendance(i, AttendanceStatus.PRESENT)); // 20
        for (int i = 21; i <= 22; i++) attendances.add(createAttendance(i, AttendanceStatus.HALF_DAY)); // 2 * 0.5 = 1.0
        for (int i = 23; i <= 25; i++) attendances.add(createAttendance(i, AttendanceStatus.ABSENT)); // 3 * 0 = 0
        
        // Total factor = 21.0
        when(attendanceRepository.findByWorkOrderIdOrderByAttendanceDateDesc(1L)).thenReturn(attendances);
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArguments()[0]);

        Settlement settlement = settlementService.calculateAndSaveSettlement(1L, 8, 2026);
        
        // 500 * 21 = 10500
        assertEquals(new BigDecimal("10500.00"), settlement.getWorkerPayableAmount());
        assertEquals(new BigDecimal("750.00"), settlement.getNewtronCommissionAmount());
        assertEquals(new BigDecimal("11250.00"), settlement.getTotalClientPayableAmount());
    }

    @Test
    void testCappedAtMonthlySalary_31Days() {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        List<Attendance> attendances = new ArrayList<>();
        for (int i = 1; i <= 31; i++) attendances.add(createAttendance(i, AttendanceStatus.PRESENT));
        
        when(attendanceRepository.findByWorkOrderIdOrderByAttendanceDateDesc(1L)).thenReturn(attendances);
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArguments()[0]);

        Settlement settlement = settlementService.calculateAndSaveSettlement(1L, 8, 2026);
        
        // 500 * 31 = 15500, but capped at 15000
        assertEquals(new BigDecimal("15000.00"), settlement.getWorkerPayableAmount());
        assertEquals(new BigDecimal("750.00"), settlement.getNewtronCommissionAmount());
        assertEquals(new BigDecimal("15750.00"), settlement.getTotalClientPayableAmount());
    }

    @Test
    void test29DaysAttendance() {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        List<Attendance> attendances = new ArrayList<>();
        for (int i = 1; i <= 29; i++) attendances.add(createAttendance(i, AttendanceStatus.PRESENT));
        
        when(attendanceRepository.findByWorkOrderIdOrderByAttendanceDateDesc(1L)).thenReturn(attendances);
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArguments()[0]);

        Settlement settlement = settlementService.calculateAndSaveSettlement(1L, 8, 2026);
        
        // 500 * 29 = 14500
        assertEquals(new BigDecimal("14500.00"), settlement.getWorkerPayableAmount());
    }

    @Test
    void test28DaysAttendance() {
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(validWorkOrder));
        List<Attendance> attendances = new ArrayList<>();
        for (int i = 1; i <= 28; i++) attendances.add(createAttendance(i, AttendanceStatus.PRESENT));
        
        when(attendanceRepository.findByWorkOrderIdOrderByAttendanceDateDesc(1L)).thenReturn(attendances);
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(i -> i.getArguments()[0]);

        Settlement settlement = settlementService.calculateAndSaveSettlement(1L, 8, 2026);
        
        // 500 * 28 = 14000
        assertEquals(new BigDecimal("14000.00"), settlement.getWorkerPayableAmount());
    }
}
