package com.newtron.newtron_workforce_backend.common.exception;

public class ConflictException extends BusinessException {
    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
