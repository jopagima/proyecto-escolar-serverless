package com.jopagima.school.courses.domain.valueobjects;

import com.jopagima.school.commons.domain.ValidationError;

/**
 * Value Object (see guidelinesHexagonal-serverless.md §4.3): the maximum number of
 * seats of a Course. Carries the business invariant that a course cannot have zero or
 * negative seats, previously validated inline in Course.create().
 */
public final class CourseCapacity {

    private final int value;

    private CourseCapacity(int value) {
        this.value = value;
    }

    public static CourseCapacity create(int value) {
        if (value <= 0) {
            throw ValidationError.create("Course max capacity must be greater than zero");
        }
        return new CourseCapacity(value);
    }

    /**
     * Rebuilds a capacity already persisted by this project — no revalidation, mirroring
     * Course.reconstitute().
     */
    public static CourseCapacity reconstitute(int value) {
        return new CourseCapacity(value);
    }

    public int value() {
        return value;
    }
}
