package com.newtron.newtron_workforce_backend.dto;

import com.newtron.newtron_workforce_backend.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class AttendanceResponseDto {
    private Long id;
    private Long workOrderId;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private String remark;
    private Instant createdAt;
    private Instant updatedAt;
}
