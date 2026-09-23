package com.jopagima.school.courses.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.jopagima.school.commons.domain.Id;

/**
 * CourseEnrollment is a domain aggregate independent of Course, even though both share
 * the same DynamoDB table via the adjacency list pattern (see CoursesTableConstruct,
 * Fase 2 Día 1). Its identity is the pair (courseId, studentId).
 */
public class CourseEnrollmentTest {
    @Test 
    void createsEnrollmentWithValidCourseAndStudent() {
        Id courseId = Id.generateUniqueIdentifier();
        CourseEnrollment enrollment = CourseEnrollment.create(courseId, "s-001");

        assertEquals(courseId, enrollment.getCourseId());
        assertEquals("s-001", enrollment.getStudentId());
    }

    @Test
    void doesNotAllowBlankCourseId() {
        assertThrows(InvalidCourseEnrollmentException.class,
                () -> CourseEnrollment.create(null, "s-001"));
    }  
    
    @Test
    void doesNotAllowBlankStudentId() {
        Id courseId = Id.generateUniqueIdentifier();
        assertThrows(InvalidCourseEnrollmentException.class,
                () -> CourseEnrollment.create(courseId, ""));
    }    
}
