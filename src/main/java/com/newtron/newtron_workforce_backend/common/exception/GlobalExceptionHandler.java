package com.newtron.newtron_workforce_backend.common.exception;

import com.newtron.newtron_workforce_backend.common.response.ApiError;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        long start = getStartTime(request);
        String reqId = getRequestId(request);
        
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> ApiError.builder()
                        .field(err.getField())
                        .code(err.getCode())
                        .message(err.getDefaultMessage())
                        .rejectedValue(err.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        ApiResponse<Object> response = ApiResponseFactory.failure(errors, "Validation failure", reqId, request.getRequestURI(), start);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        long start = getStartTime(request);
        String reqId = getRequestId(request);

        List<ApiError> errors = ex.getConstraintViolations().stream()
                .map(violation -> ApiError.builder()
                        .field(violation.getPropertyPath().toString())
                        .code("CONSTRAINT_VIOLATION")
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        ApiResponse<Object> response = ApiResponseFactory.failure(errors, "Constraint validation failure", reqId, request.getRequestURI(), start);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed or unreadable JSON payload request", ex, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        long start = getStartTime(request);
        String reqId = getRequestId(request);

        List<ApiError> errors = Collections.singletonList(
                ApiError.builder()
                        .field(ex.getName())
                        .code("TYPE_MISMATCH")
                        .message(String.format("Failed to convert value '%s' to required type '%s'", ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"))
                        .rejectedValue(ex.getValue())
                        .build()
        );

        ApiResponse<Object> response = ApiResponseFactory.failure(errors, "Parameter type mismatch", reqId, request.getRequestURI(), start);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "PATH_NOT_FOUND", "No handler found for requested resource path", ex, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredToken(ExpiredTokenException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(MalformedTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleMalformedToken(MalformedTokenException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(ConflictException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessValidation(ValidationException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access is denied for current execution privileges", ex, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", ex.getMessage(), ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleFallback(Exception ex, HttpServletRequest request) {
        log.error("Internal Server Error occurred: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please contact support.", ex, request);
    }

    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(HttpStatus status, String errorCode, String message, Exception ex, HttpServletRequest request) {
        long start = getStartTime(request);
        String reqId = getRequestId(request);

        List<ApiError> errors = Collections.singletonList(
                ApiError.builder()
                        .code(errorCode)
                        .message(message)
                        .build()
        );

        ApiResponse<Object> response = ApiResponseFactory.failure(errors, message, reqId, request.getRequestURI(), start);
        return ResponseEntity.status(status).body(response);
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        return attr instanceof Long ? (Long) attr : System.currentTimeMillis();
    }

    private String getRequestId(HttpServletRequest request) {
        String reqId = request.getHeader(REQUEST_ID_HEADER);
        if (reqId == null || reqId.trim().isEmpty()) {
            Object attr = request.getAttribute(REQUEST_ID_HEADER);
            reqId = attr instanceof String ? (String) attr : UUID.randomUUID().toString();
        }
        return reqId;
    }
}
