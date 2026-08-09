package com.newtron.newtron_workforce_backend.common.response;

import java.time.Instant;
import java.util.List;

public class ApiResponseFactory {

    private static final String API_VERSION = "v1.0";

    public static <T> ApiResponse<T> success(T data, String message, String requestId, String path, long startTimeMs) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .meta(buildMeta(requestId, path, startTimeMs))
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message, String requestId, String path, long startTimeMs) {
        return success(data, message, requestId, path, startTimeMs);
    }

    public static <T> ApiResponse<T> updated(T data, String message, String requestId, String path, long startTimeMs) {
        return success(data, message, requestId, path, startTimeMs);
    }

    public static <T> ApiResponse<T> deleted(String message, String requestId, String path, long startTimeMs) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .meta(buildMeta(requestId, path, startTimeMs))
                .build();
    }

    public static <T> ApiResponse<T> failure(List<ApiError> errors, String message, String requestId, String path, long startTimeMs) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .meta(buildMeta(requestId, path, startTimeMs))
                .build();
    }

    public static <T> ApiResponse<PageResponse<T>> paged(PageResponse<T> pagedData, String message, String requestId, String path, long startTimeMs) {
        return ApiResponse.<PageResponse<T>>builder()
                .success(true)
                .message(message)
                .data(pagedData)
                .meta(buildMeta(requestId, path, startTimeMs))
                .build();
    }

    private static MetaInfo buildMeta(String requestId, String path, long startTimeMs) {
        return MetaInfo.builder()
                .apiVersion(API_VERSION)
                .timestamp(Instant.now())
                .requestId(requestId)
                .path(path)
                .processingTimeMs(System.currentTimeMillis() - startTimeMs)
                .build();
    }
}
