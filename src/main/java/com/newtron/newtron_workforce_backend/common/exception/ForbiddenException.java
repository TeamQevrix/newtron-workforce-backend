package com.newtron.newtron_workforce_backend.common.exception;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message);
    }
}
