package com.newtron.newtron_workforce_backend.dto;

import com.newtron.newtron_workforce_backend.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDto {

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    @Size(max = 500, message = "Remark must not exceed 500 characters")
    private String remark;
}
