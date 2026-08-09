package com.newtron.newtron_workforce_backend.common.exception;

public class MalformedTokenException extends UnauthorizedException {
    public MalformedTokenException(String message) {
        super("MALFORMED_TOKEN", message);
    }
}
