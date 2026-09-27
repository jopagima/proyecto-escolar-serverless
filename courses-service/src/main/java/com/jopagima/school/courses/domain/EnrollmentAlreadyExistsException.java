package com.jopagima.school.courses.domain;

import com.jopagima.school.commons.domain.Id;
/**
 * EnrollmentAlreadyExistsException
 */
public class EnrollmentAlreadyExistsException extends RuntimeException {
    public EnrollmentAlreadyExistsException(Id courseId, Id studentId) {
        super("Enrollment already exists for course: " + courseId + " and student: " + studentId);
    }

}
