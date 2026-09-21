package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.dto.NearbyWorkerResponse;
import com.newtron.newtron_workforce_backend.service.PublicWorkerService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/workers")
@RequiredArgsConstructor
@Validated
public class PublicWorkerController {

    private final PublicWorkerService publicWorkerService;

    @GetMapping("/nearby")
    public ResponseEntity<Page<NearbyWorkerResponse>> getNearbyWorkers(
            @RequestParam @NotNull @Min(-90) @Max(90) Double latitude,
            @RequestParam @NotNull @Min(-180) @Max(180) Double longitude,
            @RequestParam(required = false, defaultValue = "10") @Positive @Max(10) Double radiusKm,
            @RequestParam(required = false) @Positive Long skillId,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NearbyWorkerResponse> nearbyWorkers = publicWorkerService.findNearbyAvailableIndividualWorkers(
                latitude, longitude, radiusKm, skillId, pageable);
        return ResponseEntity.ok(nearbyWorkers);
    }

    @GetMapping("/{workerId}")
    public com.newtron.newtron_workforce_backend.common.response.ApiResponse<com.newtron.newtron_workforce_backend.dto.PublicWorkerProfileDto> getPublicWorkerProfile(
            @PathVariable Long workerId,
            jakarta.servlet.http.HttpServletRequest request) {
        
        long startTime = System.currentTimeMillis();
        if (request.getAttribute("startTimeMs") instanceof Long) {
            startTime = (Long) request.getAttribute("startTimeMs");
        }
        
        String reqId = request.getHeader("X-Request-ID");
        if (reqId == null || reqId.trim().isEmpty()) {
            if (request.getAttribute("X-Request-ID") instanceof String) {
                reqId = (String) request.getAttribute("X-Request-ID");
            } else {
                reqId = java.util.UUID.randomUUID().toString();
            }
        }
        
        com.newtron.newtron_workforce_backend.dto.PublicWorkerProfileDto profileDto = publicWorkerService.getPublicWorkerProfile(workerId);
        
        return com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory.success(
                profileDto, 
                "Worker profile retrieved successfully", 
                reqId, 
                request.getRequestURI(), 
                startTime
        );
    }
}
