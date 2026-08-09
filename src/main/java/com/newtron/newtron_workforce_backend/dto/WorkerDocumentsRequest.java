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
public class WorkerDocumentsRequest {
    private String aadhaarCardUrl;
    private String aadhaarFileName;
    private String panCardUrl;
    private String panFileName;
    private String bankAccountHolder;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
    private String accountType;
    private String branchName;
    private String emergencyContact;
}
