package com.newtron.newtron_workforce_backend.common.validation.constants;

import java.util.regex.Pattern;

public final class ValidationPatterns {
    private ValidationPatterns() {}

    // Indian mobile: starts with 6-9 followed by 9 digits
    public static final String MOBILE_REGEX = "^[6-9]\\d{9}$";
    public static final Pattern MOBILE_PATTERN = Pattern.compile(MOBILE_REGEX);

    // Password policy: 8-32 chars, 1 upper, 1 lower, 1 digit, 1 special character
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,32}$";
    public static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    // Name policy: 2-100 characters, unicode letters, spaces, hyphens, and apostrophes. Prevents symbols-only input
    public static final String NAME_REGEX = "^[\\p{L}'’\\s\\-]{2,100}$";
    public static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX);

    // Indian pincode: 6 digits
    public static final String PINCODE_REGEX = "^\\d{6}$";
    public static final Pattern PINCODE_PATTERN = Pattern.compile(PINCODE_REGEX);
}
