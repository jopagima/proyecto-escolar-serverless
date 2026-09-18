package com.jopagima.school.courses.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * CourseEnrollment is a domain aggregate independent of Course, even though both share
 * the same DynamoDB table via the adjacency list pattern (see CoursesTableConstruct,
 * Fase 2 Día 1). Its identity is the pair (courseId, studentId).
 */
public class CourseEnrollmentTest {
    @Test 
    void createsEnrollmentWithValidCourseAndStudent() {
        CourseEnrollment enrollment = CourseEnrollment.create("c-001", "s-001");

        assertEquals("c-001", enrollment.getCourseId());
        assertEquals("s-001", enrollment.getStudentId());
    }

    @Test
    void doesNotAllowBlankCourseId() {
        assertThrows(InvalidCourseEnrollmentException.class,
                () -> CourseEnrollment.create(" ", "s-001"));
    }  
    
    @Test
    void doesNotAllowBlankStudentId() {
        assertThrows(InvalidCourseEnrollmentException.class,
                () -> CourseEnrollment.create("c-001", ""));
    }    
}
