package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.WorkerDocumentResponse;
import com.newtron.newtron_workforce_backend.entity.WorkerDocument;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import com.newtron.newtron_workforce_backend.dto.WorkerDocumentsRequest;
import com.newtron.newtron_workforce_backend.entity.WorkerBank;
import com.newtron.newtron_workforce_backend.repository.WorkerBankRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerDocumentRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.service.OnboardingProgressService;
import com.newtron.newtron_workforce_backend.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/worker/profile/documents")
@RequiredArgsConstructor
public class WorkerDocumentController {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerDocumentRepository workerDocumentRepository;
    private final WorkerBankRepository workerBankRepository;
    private final StorageService storageService;
    private final OnboardingProgressService onboardingProgressService;

    private static final String AADHAAR_DOC_TYPE = "AADHAAR_CARD";
    private static final String PAN_DOC_TYPE = "PAN_CARD";

    @PostMapping("/aadhaar")
    @Transactional
    public ApiResponse<WorkerDocumentResponse> uploadAadhaar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        // Upload the file content to storage
        String fileKey = storageService.uploadDocument(file);
        String originalFilename = file.getOriginalFilename();

        // Find existing Aadhaar document or create a new one
        Optional<WorkerDocument> existingDoc = workerDocumentRepository
                .findByWorkerProfileIdAndDocumentType(profile.getId(), AADHAAR_DOC_TYPE);

        WorkerDocument document;
        if (existingDoc.isPresent()) {
            document = existingDoc.get();
            document.setFileStorageKey(fileKey);
            document.setFileName(originalFilename);
        } else {
            document = WorkerDocument.builder()
                    .workerProfile(profile)
                    .documentType(AADHAAR_DOC_TYPE)
                    .fileStorageKey(fileKey)
                    .fileName(originalFilename)
                    .build();
        }

        WorkerDocument saved = workerDocumentRepository.save(document);

        WorkerDocumentResponse response = WorkerDocumentResponse.builder()
                .documentType(saved.getDocumentType())
                .fileKey(saved.getFileStorageKey())
                .fileUrl(storageService.generateDownloadUrl(saved.getFileStorageKey()))
                .fileName(saved.getFileName())
                .build();

        return ApiResponseFactory.success(response, "Aadhaar Card uploaded successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/aadhaar")
    public ApiResponse<WorkerDocumentResponse> getAadhaar(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        WorkerDocument document = workerDocumentRepository
                .findByWorkerProfileIdAndDocumentType(profile.getId(), AADHAAR_DOC_TYPE)
                .orElseThrow(() -> new ResourceNotFoundException("AADHAAR_NOT_FOUND", "Aadhaar Card has not been uploaded yet"));

        String originalName = document.getFileName() != null ? document.getFileName() :
                document.getFileStorageKey().substring(document.getFileStorageKey().lastIndexOf("/") + 1);

        WorkerDocumentResponse response = WorkerDocumentResponse.builder()
                .documentType(document.getDocumentType())
                .fileKey(document.getFileStorageKey())
                .fileUrl(storageService.generateDownloadUrl(document.getFileStorageKey()))
                .fileName(originalName)
                .build();

        return ApiResponseFactory.success(response, "Aadhaar Card retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/pan")
    @Transactional
    public ApiResponse<WorkerDocumentResponse> uploadPan(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        String fileKey = storageService.uploadDocument(file);
        String originalFilename = file.getOriginalFilename();

        Optional<WorkerDocument> existingDoc = workerDocumentRepository
                .findByWorkerProfileIdAndDocumentType(profile.getId(), PAN_DOC_TYPE);

        WorkerDocument document;
        if (existingDoc.isPresent()) {
            document = existingDoc.get();
            document.setFileStorageKey(fileKey);
            document.setFileName(originalFilename);
        } else {
            document = WorkerDocument.builder()
                    .workerProfile(profile)
                    .documentType(PAN_DOC_TYPE)
                    .fileStorageKey(fileKey)
                    .fileName(originalFilename)
                    .build();
        }

        WorkerDocument saved = workerDocumentRepository.save(document);

        WorkerDocumentResponse response = WorkerDocumentResponse.builder()
                .documentType(saved.getDocumentType())
                .fileKey(saved.getFileStorageKey())
                .fileUrl(storageService.generateDownloadUrl(saved.getFileStorageKey()))
                .fileName(saved.getFileName())
                .build();

        return ApiResponseFactory.success(response, "PAN Card uploaded successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/pan")
    public ApiResponse<WorkerDocumentResponse> getPan(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        WorkerDocument document = workerDocumentRepository
                .findByWorkerProfileIdAndDocumentType(profile.getId(), PAN_DOC_TYPE)
                .orElseThrow(() -> new ResourceNotFoundException("PAN_NOT_FOUND", "PAN Card has not been uploaded yet"));

        String originalName = document.getFileName() != null ? document.getFileName() :
                document.getFileStorageKey().substring(document.getFileStorageKey().lastIndexOf("/") + 1);

        WorkerDocumentResponse response = WorkerDocumentResponse.builder()
                .documentType(document.getDocumentType())
                .fileKey(document.getFileStorageKey())
                .fileUrl(storageService.generateDownloadUrl(document.getFileStorageKey()))
                .fileName(originalName)
                .build();

        return ApiResponseFactory.success(response, "PAN Card retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping
    @Transactional
    public ApiResponse<Void> saveDocuments(
            @RequestBody WorkerDocumentsRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        // Save Emergency Contact
        if (request.getEmergencyContact() == null || request.getEmergencyContact().trim().length() != 10) {
            throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("INVALID_EMERGENCY_CONTACT", "Valid 10-digit emergency contact is required");
        }
        profile.setEmergencyContact(request.getEmergencyContact().trim());

        // Save Bank Details (Optional)
        boolean hasBankInfo = (request.getBankAccountHolder() != null && !request.getBankAccountHolder().trim().isEmpty())
                || (request.getBankAccountNumber() != null && !request.getBankAccountNumber().trim().isEmpty())
                || (request.getBankIfscCode() != null && !request.getBankIfscCode().trim().isEmpty())
                || (request.getBankName() != null && !request.getBankName().trim().isEmpty())
                || (request.getAccountType() != null && !request.getAccountType().trim().isEmpty());

        if (hasBankInfo) {
            if (request.getBankAccountHolder() == null || request.getBankAccountHolder().trim().isEmpty()) {
                throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("BANK_HOLDER_REQUIRED", "Account holder name is required");
            }
            if (request.getBankAccountNumber() == null || request.getBankAccountNumber().trim().isEmpty()) {
                throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("BANK_ACCOUNT_REQUIRED", "Account number is required");
            }
            if (request.getBankIfscCode() == null || request.getBankIfscCode().trim().isEmpty()) {
                throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("BANK_IFSC_REQUIRED", "IFSC code is required");
            }
            if (request.getBankName() == null || request.getBankName().trim().isEmpty()) {
                throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("BANK_NAME_REQUIRED", "Bank name is required");
            }
            if (request.getAccountType() == null || request.getAccountType().trim().isEmpty()) {
                throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("BANK_TYPE_REQUIRED", "Account type is required");
            }

            WorkerBank bank = workerBankRepository.findByWorkerProfileId(profile.getId())
                    .orElse(new WorkerBank());
            bank.setWorkerProfile(profile);
            bank.setAccountHolderName(request.getBankAccountHolder().trim());
            bank.setAccountNumber(request.getBankAccountNumber().trim());
            bank.setBankName(request.getBankName().trim());
            bank.setIfscCode(request.getBankIfscCode().trim());
            bank.setAccountType(request.getAccountType().trim().toUpperCase());
            bank.setBranchName(request.getBranchName() != null ? request.getBranchName().trim() : null);

            workerBankRepository.save(bank);
        } else {
            workerBankRepository.findByWorkerProfileId(profile.getId())
                    .ifPresent(workerBankRepository::delete);
        }

        // Update onboarding progress to next step
        OnboardingStep nextStep = onboardingProgressService.getNextStep(profile);
        profile.setCurrentStep(nextStep);
        profile.setIsCompleted(nextStep == OnboardingStep.COMPLETED);
        workerProfileRepository.save(profile);

        return ApiResponseFactory.success(null, "Documents saved and step advanced successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<WorkerDocumentsRequest> getDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        WorkerDocument aadhaarDoc = workerDocumentRepository.findByWorkerProfileIdAndDocumentType(profile.getId(), AADHAAR_DOC_TYPE).orElse(null);
        String aadhaarUrl = aadhaarDoc != null ? storageService.generateDownloadUrl(aadhaarDoc.getFileStorageKey()) : null;
        String aadhaarName = aadhaarDoc != null ? (aadhaarDoc.getFileName() != null ? aadhaarDoc.getFileName() :
                aadhaarDoc.getFileStorageKey().substring(aadhaarDoc.getFileStorageKey().lastIndexOf("/") + 1)) : null;

        WorkerDocument panDoc = workerDocumentRepository.findByWorkerProfileIdAndDocumentType(profile.getId(), PAN_DOC_TYPE).orElse(null);
        String panUrl = panDoc != null ? storageService.generateDownloadUrl(panDoc.getFileStorageKey()) : null;
        String panName = panDoc != null ? (panDoc.getFileName() != null ? panDoc.getFileName() :
                panDoc.getFileStorageKey().substring(panDoc.getFileStorageKey().lastIndexOf("/") + 1)) : null;

        WorkerBank bank = workerBankRepository.findByWorkerProfileId(profile.getId()).orElse(null);

        WorkerDocumentsRequest response = WorkerDocumentsRequest.builder()
                .aadhaarCardUrl(aadhaarUrl)
                .aadhaarFileName(aadhaarName)
                .panCardUrl(panUrl)
                .panFileName(panName)
                .bankAccountHolder(bank != null ? bank.getAccountHolderName() : null)
                .bankAccountNumber(bank != null ? bank.getAccountNumber() : null)
                .bankIfscCode(bank != null ? bank.getIfscCode() : null)
                .bankName(bank != null ? bank.getBankName() : null)
                .accountType(bank != null ? bank.getAccountType() : null)
                .branchName(bank != null ? bank.getBranchName() : null)
                .emergencyContact(profile.getEmergencyContact())
                .build();

        return ApiResponseFactory.success(response, "Documents retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/{documentType}/view")
    public ResponseEntity<byte[]> viewDocument(
            @PathVariable("documentType") String documentType,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        String lookupType = documentType.toUpperCase();
        if (lookupType.equals("AADHAAR")) {
            lookupType = AADHAAR_DOC_TYPE;
        } else if (lookupType.equals("PAN")) {
            lookupType = PAN_DOC_TYPE;
        }

        WorkerDocument document = workerDocumentRepository
                .findByWorkerProfileIdAndDocumentType(profile.getId(), lookupType)
                .orElseThrow(() -> new ResourceNotFoundException("DOCUMENT_NOT_FOUND", "Document not found"));

        String fileKey = document.getFileStorageKey();
        byte[] fileBytes = storageService.getDocumentContent(fileKey);
        String contentType = storageService.getDocumentContentType(fileKey);

        if (fileBytes == null) {
            throw new ResourceNotFoundException("FILE_CONTENT_NOT_FOUND", "File content was not found or is empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(fileKey.substring(fileKey.lastIndexOf("/") + 1))
                .build());

        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }

    private User fetchCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private WorkerProfile fetchWorkerProfile(User user) {
        return workerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found"));
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return System.currentTimeMillis();
    }
}
