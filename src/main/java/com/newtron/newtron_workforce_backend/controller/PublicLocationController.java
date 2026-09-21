package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.util.IpRateLimiter;
import com.newtron.newtron_workforce_backend.common.exception.BusinessException;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.LocationSearchResultDto;
import com.newtron.newtron_workforce_backend.service.LocationProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/location")
public class PublicLocationController {

    private final LocationProxyService locationProxyService;
    private final IpRateLimiter ipRateLimiter;
    
    public PublicLocationController(LocationProxyService locationProxyService, IpRateLimiter ipRateLimiter) {
        this.locationProxyService = locationProxyService;
        this.ipRateLimiter = ipRateLimiter;
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LocationSearchResultDto>>> searchLocation(
            @RequestParam("query") String query,
            HttpServletRequest request) {
            
        String clientIp = request.getRemoteAddr();
        // Allow max 10 requests per minute for location search from same IP
        if (!ipRateLimiter.isAllowed(clientIp, 10, 60000)) {
            throw new BusinessException("RATE_LIMIT_EXCEEDED", "Too many search requests. Please try again later.");
        }
        
        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException("INVALID_QUERY", "Search query cannot be empty");
        }
        
        if (query.trim().length() > 100) {
            throw new BusinessException("INVALID_QUERY", "Search query is too long");
        }
        
        List<LocationSearchResultDto> results = locationProxyService.searchLocation(query);
        
        long startTime = getStartTime(request);
        String reqId = getRequestId(request);
        
        ApiResponse<List<LocationSearchResultDto>> response = ApiResponseFactory.success(
                results, 
                "Locations retrieved successfully", 
                reqId, 
                request.getRequestURI(), 
                startTime
        );
        
        return ResponseEntity.ok(response);
    }
    
    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        return attr instanceof Long ? (Long) attr : System.currentTimeMillis();
    }

    private String getRequestId(HttpServletRequest request) {
        String reqId = request.getHeader("X-Request-ID");
        if (reqId == null || reqId.trim().isEmpty()) {
            Object attr = request.getAttribute("X-Request-ID");
            reqId = attr instanceof String ? (String) attr : UUID.randomUUID().toString();
        }
        return reqId;
    }
}
