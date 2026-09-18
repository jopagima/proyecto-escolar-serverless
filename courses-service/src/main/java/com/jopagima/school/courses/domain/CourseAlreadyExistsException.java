package com.jopagima.school.courses.domain;

/**
 * CourseAlreadyExistsException
 */
public class CourseAlreadyExistsException extends RuntimeException{
    public CourseAlreadyExistsException(String courseId) {
        super("Course already exists: " + courseId);
    }
}
