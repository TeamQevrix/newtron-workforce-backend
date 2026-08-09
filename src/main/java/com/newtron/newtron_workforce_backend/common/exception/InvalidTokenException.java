package com.newtron.newtron_workforce_backend.common.exception;

public class InvalidTokenException extends UnauthorizedException {
    public InvalidTokenException(String message) {
        super("INVALID_TOKEN", message);
    }
}
