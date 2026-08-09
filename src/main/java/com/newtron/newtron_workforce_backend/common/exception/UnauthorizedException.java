package com.newtron.newtron_workforce_backend.common.exception;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }
}
