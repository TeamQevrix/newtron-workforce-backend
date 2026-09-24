package com.newtron.newtron_workforce_backend.dto;

import com.newtron.newtron_workforce_backend.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentralAttendanceResponseDto {
    private Long id;
    private Long workOrderId;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private String remark;
    private String workOrderNumber;
    private String workerName;
    private Long workerProfileId;
}
