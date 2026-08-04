package com.example.plimap.global.external.storage;

import java.util.regex.Pattern;

public final class ProfileImageObjectKeyValidator {

    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile(
            "members/[1-9]\\d*/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}"
                    + "-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.webp"
    );

    private ProfileImageObjectKeyValidator() {
    }

    public static void validate(String objectKey) {
        if (objectKey == null || !OBJECT_KEY_PATTERN.matcher(objectKey).matches()) {
            throw new IllegalArgumentException("Invalid profile image object key");
        }
    }
}