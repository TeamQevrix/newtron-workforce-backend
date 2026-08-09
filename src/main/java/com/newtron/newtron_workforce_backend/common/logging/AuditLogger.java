package com.newtron.newtron_workforce_backend.common.logging;

import lombok.extern.slf4j.Slf4j;
import java.time.Instant;

@Slf4j
public class AuditLogger {

    public static void logAction(String action, String userId, String details) {
        String requestId = RequestContext.getRequestId() != null ? RequestContext.getRequestId() : "SYSTEM";
        log.info("{\"timestamp\":\"{}\",\"level\":\"INFO\",\"requestId\":\"{}\",\"userId\":\"{}\",\"action\":\"{}\",\"details\":\"{}\"}",
                Instant.now(), requestId, userId != null ? userId : "SYSTEM", action, escapeJson(details));
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"");
    }
}
