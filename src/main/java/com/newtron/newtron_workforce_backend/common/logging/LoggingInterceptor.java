package com.newtron.newtron_workforce_backend.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Set user context if authenticated
        String user = request.getRemoteUser();
        if (user != null) {
            RequestContext.setUserId(user);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        long startTime = getStartTime(request);
        long execTime = System.currentTimeMillis() - startTime;

        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();
        String ip = request.getRemoteAddr();
        String userId = RequestContext.getUserId() != null ? RequestContext.getUserId() : "anonymous";
        String requestId = RequestContext.getRequestId() != null ? RequestContext.getRequestId() : "N/A";

        // Structured JSON styled production log output formatting
        log.info("{\"timestamp\":\"{}\",\"level\":\"INFO\",\"requestId\":\"{}\",\"userId\":\"{}\",\"method\":\"{}\",\"path\":\"{}\",\"status\":{},\"executionTimeMs\":{},\"ip\":\"{}\"}",
                java.time.Instant.now(), requestId, userId, method, path, status, execTime, ip);
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        return attr instanceof Long ? (Long) attr : System.currentTimeMillis();
    }
}
