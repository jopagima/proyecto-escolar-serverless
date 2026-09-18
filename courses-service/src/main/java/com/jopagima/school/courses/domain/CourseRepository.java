package com.jopagima.school.courses.domain;

/**
 * Port: the domain depends on this interface, never on a concrete AWS SDK type.
 * Enrollment operations are intentionally not part of this port yet — they belong to
 * a future day, once CourseEnrollment as a concept is modeled.
 */
public interface CourseRepository {
    void save(Course course) throws CourseAlreadyExistsException;
}
