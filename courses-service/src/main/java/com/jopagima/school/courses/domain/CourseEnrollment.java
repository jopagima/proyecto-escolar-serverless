package com.jopagima.school.courses.domain;

import com.jopagima.school.commons.domain.Id;

/**
 * Design decision: CourseEnrollment is an independent domain aggregate from Course,
 * even though both persist as items in the same DynamoDB table under the same PK
 * (adjacency list pattern, see CoursesTableConstruct). Its identity is the pair
 * (courseId, studentId), not a generated id of its own.
 */
public class CourseEnrollment {

    private final Id courseId;
    private final String studentId;

    private CourseEnrollment(Id courseId, String studentId) {
        this.courseId = courseId;
        this.studentId = studentId;
    }


    public Id getCourseId() {
        return courseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public static CourseEnrollment create(Id courseId, String studentId) {
        if (courseId == null) {
            throw new InvalidCourseEnrollmentException("Course ID cannot be blank");    
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new InvalidCourseEnrollmentException("Student ID cannot be blank");
        }
           return new CourseEnrollment(courseId, studentId);
    }

}
