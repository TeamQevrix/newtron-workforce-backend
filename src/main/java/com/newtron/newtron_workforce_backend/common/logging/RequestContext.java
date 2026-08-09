package com.newtron.newtron_workforce_backend.common.logging;

public class RequestContext {

    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    public static void setUserId(String userId) {
        CURRENT_USER.set(userId);
    }

    public static String getUserId() {
        return CURRENT_USER.get();
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
        REQUEST_ID.remove();
    }
}
