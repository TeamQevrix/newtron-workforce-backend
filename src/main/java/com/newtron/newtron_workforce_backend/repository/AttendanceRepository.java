package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByWorkOrderIdAndAttendanceDate(Long workOrderId, LocalDate attendanceDate);

    List<Attendance> findByWorkOrderIdOrderByAttendanceDateDesc(Long workOrderId);

    @org.springframework.data.jpa.repository.Query("SELECT new com.newtron.newtron_workforce_backend.dto.CentralAttendanceResponseDto(" +
            "a.id, a.workOrder.id, a.attendanceDate, a.status, a.remark, " +
            "wo.workOrderNumber, wp.fullName, wp.id) " +
            "FROM Attendance a " +
            "JOIN a.workOrder wo " +
            "JOIN WorkerProfile wp ON wp.user = wo.worker " +
            "WHERE wo.company.id = :companyId " +
            "AND (:date IS NULL OR a.attendanceDate = :date) " +
            "ORDER BY a.attendanceDate DESC, a.id DESC")
    org.springframework.data.domain.Page<com.newtron.newtron_workforce_backend.dto.CentralAttendanceResponseDto> findCentralAttendanceByCompanyId(
            @org.springframework.data.repository.query.Param("companyId") Long companyId,
            @org.springframework.data.repository.query.Param("date") LocalDate date,
            org.springframework.data.domain.Pageable pageable);
}
