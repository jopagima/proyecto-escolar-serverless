package com.jopagima.school.courses.domain;

import java.util.Optional;

import com.jopagima.school.commons.domain.Id;

/**
 * Port: the domain depends on this interface, never on a concrete AWS SDK type.
 * Enrollment operations are intentionally not part of this port yet — they belong to
 * a future day, once CourseEnrollment as a concept is modeled.
 */
public interface CourseRepository {
    void save(Course course) throws CourseAlreadyExistsException;

    Optional<Course> findById(Id id);
}
