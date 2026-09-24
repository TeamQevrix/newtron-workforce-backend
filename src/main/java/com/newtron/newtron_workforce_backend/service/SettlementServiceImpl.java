package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.entity.Attendance;
import com.newtron.newtron_workforce_backend.entity.Settlement;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.enums.AttendanceStatus;
import com.newtron.newtron_workforce_backend.repository.AttendanceRepository;
import com.newtron.newtron_workforce_backend.repository.SettlementRepository;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    @Transactional
    public Settlement calculateAndSaveSettlement(Long workOrderId, int month, int year) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid settlement month");
        }

        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("WorkOrder not found"));

        if (!"MONTHLY".equalsIgnoreCase(workOrder.getEngagementType())) {
            throw new IllegalArgumentException("Only MONTHLY engagement is accepted in this phase");
        }

        if (settlementRepository.findByWorkOrderIdAndSettlementMonthAndSettlementYearAndDeletedFalse(workOrderId, month, year).isPresent()) {
            throw new IllegalStateException("Duplicate settlement rejected for this month and year");
        }

        BigDecimal monthlySalary = workOrder.getMonthlySalary();
        if (monthlySalary == null) {
            throw new IllegalStateException("WorkOrder monthly salary is required for settlement");
        }

        BigDecimal commissionRate = workOrder.getCommissionRate();
        if (commissionRate == null) {
            throw new IllegalStateException("WorkOrder commission rate is required for settlement");
        }

        List<Attendance> allAttendance = attendanceRepository.findByWorkOrderIdOrderByAttendanceDateDesc(workOrderId);
        List<Attendance> monthlyAttendance = allAttendance.stream()
                .filter(a -> {
                    LocalDate date = a.getAttendanceDate();
                    return date.getMonthValue() == month && date.getYear() == year;
                })
                .collect(Collectors.toList());

        BigDecimal attendanceFactorSum = BigDecimal.ZERO;

        for (Attendance a : monthlyAttendance) {
            if (a.getStatus() == AttendanceStatus.PRESENT) {
                attendanceFactorSum = attendanceFactorSum.add(BigDecimal.ONE);
            } else if (a.getStatus() == AttendanceStatus.HALF_DAY) {
                attendanceFactorSum = attendanceFactorSum.add(new BigDecimal("0.5"));
            } else if (a.getStatus() == AttendanceStatus.ABSENT) {
                attendanceFactorSum = attendanceFactorSum.add(BigDecimal.ZERO);
            }
        }

        BigDecimal dailyRate = monthlySalary.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
        BigDecimal calculatedWorkerPayable = dailyRate.multiply(attendanceFactorSum).setScale(2, RoundingMode.HALF_UP);
        BigDecimal workerPayableAmount = calculatedWorkerPayable.min(monthlySalary);

        BigDecimal newtronCommissionAmount = workOrder.getCommissionAmount();
        if (newtronCommissionAmount == null) {
            newtronCommissionAmount = BigDecimal.ZERO;
        }

        BigDecimal totalClientPayableAmount = workerPayableAmount.add(newtronCommissionAmount);

        Settlement settlement = Settlement.builder()
                .workOrder(workOrder)
                .settlementMonth(month)
                .settlementYear(year)
                .workerPayableAmount(workerPayableAmount)
                .newtronCommissionAmount(newtronCommissionAmount)
                .totalClientPayableAmount(totalClientPayableAmount)
                .build();

        return settlementRepository.save(settlement);
    }
}
