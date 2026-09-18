package com.jopagima.school.courses.domain;

/**
 * Design decision: CourseEnrollment is an independent domain aggregate from Course,
 * even though both persist as items in the same DynamoDB table under the same PK
 * (adjacency list pattern, see CoursesTableConstruct). Its identity is the pair
 * (courseId, studentId), not a generated id of its own.
 */
public class CourseEnrollment {

    private final String courseId;
    private final String studentId;

    private CourseEnrollment(String courseId, String studentId) {
        this.courseId = courseId;
        this.studentId = studentId;
    }


    public String getCourseId() {
        return courseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public static CourseEnrollment create(String courseId, String studentId) {
        if (courseId == null || courseId.trim().isEmpty()) {
            throw new InvalidCourseEnrollmentException("Course ID cannot be blank");    
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new InvalidCourseEnrollmentException("Student ID cannot be blank");
        }
           return new CourseEnrollment(courseId, studentId);
    }

}
