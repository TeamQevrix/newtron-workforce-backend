package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.RecruiterCompanyProfileRequest;
import com.newtron.newtron_workforce_backend.dto.RecruiterCompanyProfileResponse;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiter/company")
@RequiredArgsConstructor
public class RecruiterCompanyController {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @PutMapping
    @Transactional
    public ApiResponse<RecruiterCompanyProfileResponse> updateCompanyProfile(
            @Valid @RequestBody RecruiterCompanyProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can configure company profiles.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId()).orElse(null);
        boolean isNew = false;

        if (company == null) {
            isNew = true;
            company = new Company();
            company.setOwner(currentUser);
        }

        company.setCompanyName(request.getCompanyName().trim());
        company.setContactPersonName(request.getContactPersonName().trim());
        company.setContactMobile(request.getContactMobile().trim());
        company.setCity(request.getCity().trim());
        company.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        company.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        company.setPincode(request.getPincode() != null ? request.getPincode().trim() : null);
        company.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        company.setProfileCompleted(true);

        Company savedCompany = companyRepository.save(company);

        // Update User profile completed state and full name to align GoRouter redirects and dashboard greetings
        currentUser.setFullName(request.getContactPersonName().trim());
        if (isNew || !currentUser.getProfileCompleted()) {
            currentUser.setProfileCompleted(true);
        }
        userRepository.save(currentUser);

        RecruiterCompanyProfileResponse response = mapToResponse(savedCompany);

        String message = isNew ? "Company profile created successfully" : "Company profile updated successfully";
        return ApiResponseFactory.success(response, message, RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping
    public ApiResponse<RecruiterCompanyProfileResponse> getCompanyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can retrieve company profiles.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("COMPANY_NOT_FOUND", "Company profile not found"));

        if (currentUser.getFullName() == null || currentUser.getFullName().trim().isEmpty()) {
            if (company.getContactPersonName() != null && !company.getContactPersonName().trim().isEmpty()) {
                currentUser.setFullName(company.getContactPersonName().trim());
                userRepository.save(currentUser);
            }
        }

        RecruiterCompanyProfileResponse response = mapToResponse(company);

        return ApiResponseFactory.success(response, "Company profile retrieved successfully", RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(), startTime);
    }

    private RecruiterCompanyProfileResponse mapToResponse(Company company) {
        return RecruiterCompanyProfileResponse.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .contactPersonName(company.getContactPersonName())
                .contactMobile(company.getContactMobile())
                .email(company.getEmail())
                .city(company.getCity())
                .address(company.getAddress())
                .pincode(company.getPincode())
                .description(company.getDescription())
                .profileCompleted(company.getProfileCompleted())
                .build();
    }

    private User fetchCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return System.currentTimeMillis();
    }
}
