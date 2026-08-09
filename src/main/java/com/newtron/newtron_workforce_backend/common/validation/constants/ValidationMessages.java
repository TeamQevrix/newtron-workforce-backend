package com.newtron.newtron_workforce_backend.common.validation.constants;

public final class ValidationMessages {
    private ValidationMessages() {}

    public static final String INVALID_MOBILE = "Mobile number must be a valid 10-digit Indian mobile starting with 6, 7, 8, or 9";
    public static final String INVALID_PASSWORD = "Password must be 8-32 characters long, containing at least one uppercase letter, one lowercase letter, one digit, and one special character";
    public static final String INVALID_NAME = "Name must be 2-100 characters long and contain only letters and space/symbol punctuation";
    public static final String INVALID_PINCODE = "Pincode must be exactly 6 digits";
    public static final String INVALID_AGE = "Age must be between 18 and 70 years old";
}
