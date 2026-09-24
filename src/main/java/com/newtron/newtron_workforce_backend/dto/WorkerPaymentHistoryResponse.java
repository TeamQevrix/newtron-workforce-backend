package com.newtron.newtron_workforce_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkerPaymentHistoryResponse {
    private Long id;
    private Long amount;
    private String currency;
    private String status;
    private LocalDateTime paidAt;
    private String paymentId;
    private String orderId;
    private String paymentMethod;
}
