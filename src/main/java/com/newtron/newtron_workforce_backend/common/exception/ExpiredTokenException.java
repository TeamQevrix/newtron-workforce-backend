package com.newtron.newtron_workforce_backend.common.exception;

public class ExpiredTokenException extends UnauthorizedException {
    public ExpiredTokenException(String message) {
        super("EXPIRED_TOKEN", message);
    }
}
