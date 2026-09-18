package com.newtron.newtron_workforce_backend.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobHistoryResponseDto {
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String location;
    private String salary;
    private Instant hiredDate;
    private Instant completionDate;
    private String status;
    private String duration;
    private BigDecimal earnings;
    private Integer rating;
    private String comment;
}
