package com.jopagima.school.courses.domain.repositories;

import java.util.Optional;

import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.courses.domain.entities.Course;

/**
 * Port: the domain depends on this interface, never on a concrete AWS SDK type.
 * Enrollment operations live in a separate port, CourseEnrollmentRepository, because
 * CourseEnrollment is an independent domain aggregate from Course (see
 * CourseEnrollment's own Javadoc) — not because they're missing from here.
 */
public interface CourseRepository {
    void save(Course course);

    Optional<Course> findById(Id id);
}
