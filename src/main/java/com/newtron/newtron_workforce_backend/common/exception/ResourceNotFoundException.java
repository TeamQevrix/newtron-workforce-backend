package com.newtron.newtron_workforce_backend.common.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
