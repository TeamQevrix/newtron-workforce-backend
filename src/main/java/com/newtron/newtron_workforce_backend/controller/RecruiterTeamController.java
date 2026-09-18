package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.TeamRegistrationResponse;
import com.newtron.newtron_workforce_backend.service.RecruiterTeamService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiter/teams")
@RequiredArgsConstructor
public class RecruiterTeamController {

    private final RecruiterTeamService recruiterTeamService;

    @GetMapping
    public ApiResponse<Page<TeamRegistrationResponse>> discoverTeams(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long skill,
            @RequestParam(required = false) Long state,
            @RequestParam(required = false) Long district,
            @RequestParam(required = false) Long city,
            @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest httpServletRequest) {
            
        long startTime = System.currentTimeMillis();

        Page<TeamRegistrationResponse> response = recruiterTeamService.discoverTeams(
                name,
                skill,
                state,
                district,
                city,
                pageable
        );

        return ApiResponseFactory.success(
                response,
                "Teams discovered successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }
}
