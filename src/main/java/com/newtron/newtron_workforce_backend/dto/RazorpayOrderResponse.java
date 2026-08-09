package com.newtron.newtron_workforce_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayOrderResponse {
    private String orderId;
    private Long amount;
    private String currency;
    private String keyId;
}
