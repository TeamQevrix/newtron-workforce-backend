package com.newtron.newtron_workforce_backend.common.exception;

public class ValidationException extends BusinessException {
    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
