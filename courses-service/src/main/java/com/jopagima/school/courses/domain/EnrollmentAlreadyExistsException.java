package com.jopagima.school.courses.domain;

/**
 * EnrollmentAlreadyExistsException
 */
public class EnrollmentAlreadyExistsException extends RuntimeException {
    public EnrollmentAlreadyExistsException(String courseId, String studentId) {
        super("Enrollment already exists for course: " + courseId + " and student: " + studentId);
    }

}
