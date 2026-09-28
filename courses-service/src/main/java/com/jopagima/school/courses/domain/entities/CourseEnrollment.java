package com.jopagima.school.courses.domain.entities;

import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.commons.domain.ValidationError;

/**
 * Design decision: CourseEnrollment is an independent domain aggregate from Course,
 * even though both persist as items in the same DynamoDB table under the same PK
 * (adjacency list pattern, see CoursesTableConstruct). Its identity is the pair
 * (courseId, studentId), not a generated id of its own.
 */
public class CourseEnrollment {

    private final Id courseId;
    private final Id studentId;

    private CourseEnrollment(Id courseId, Id studentId) {
        this.courseId = courseId;
        this.studentId = studentId;
    }


    public Id getCourseId() {
        return courseId;
    }

    public Id getStudentId() {
        return studentId;
    }

    public static CourseEnrollment create(Id courseId, Id studentId) {
        if (courseId == null) {
            throw ValidationError.create("Course ID cannot be blank");    
        }
        if (studentId == null) {
            throw ValidationError.create("Student ID cannot be blank");
        }
           return new CourseEnrollment(courseId, studentId);
    }

}
