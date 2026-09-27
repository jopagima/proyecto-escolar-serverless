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
        Id studentId = Id.generateUniqueIdentifier();
        CourseEnrollment enrollment = CourseEnrollment.create(courseId, studentId);

        assertEquals(courseId, enrollment.getCourseId());
        assertEquals(studentId, enrollment.getStudentId());
    }

    @Test
    void doesNotAllowBlankCourseId() {
        Id studentId = Id.generateUniqueIdentifier();
        assertThrows(InvalidCourseEnrollmentException.class,
                () -> CourseEnrollment.create(null, studentId));
    }  
    
    @Test
    void doesNotAllowBlankStudentId() {
        Id courseId = Id.generateUniqueIdentifier();
        assertThrows(InvalidCourseEnrollmentException.class,
                () -> CourseEnrollment.create(courseId, null));
    }    
}
