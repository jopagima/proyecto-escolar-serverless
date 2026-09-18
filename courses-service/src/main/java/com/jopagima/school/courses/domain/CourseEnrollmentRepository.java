package com.jopagima.school.courses.domain;



/**
 * Port: enroll() persists the enrollment (unique per courseId+studentId, enforced via
 * a conditional write). countEnrollments() supports EnrollmentEligibilityService by
 * answering "how many students are currently enrolled in this course" — a read the
 * adjacency list pattern (Fase 2 Día 1) makes cheap via a single Query.
 */
public interface CourseEnrollmentRepository {
    void enroll(CourseEnrollment enrollment) throws EnrollmentAlreadyExistsException;

    int countEnrollments(String courseId);


}
