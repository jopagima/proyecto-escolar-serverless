package com.jopagima.school.students.domain.valueobjects;

import java.util.regex.Pattern;

import com.jopagima.school.commons.domain.ValidationError;

/**
 * Value Object (see guidelinesHexagonal-serverless.md §4.3): a student's contact email.
 * Carries the format invariant previously validated inline in Student.create().
 */
public final class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email create(String value) {
        if (value == null || value.isBlank()) {
            throw ValidationError.create("email cannot be blank");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw ValidationError.create("email must be valid");
        }
        return new Email(value);
    }

    public String value() {
        return value;
    }
}
