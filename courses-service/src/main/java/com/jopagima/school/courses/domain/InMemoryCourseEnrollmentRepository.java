package com.jopagima.school.courses.domain;

import java.util.List;
import java.util.ArrayList;
import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.courses.domain.CourseEnrollment;

public class InMemoryCourseEnrollmentRepository implements CourseEnrollmentRepository {

    private final List<CourseEnrollment> enrollments = new ArrayList<>();

    @Override
    public void enroll(CourseEnrollment enrollment) {
        enrollments.add(enrollment);
    }

    @Override
    public int countEnrollments(Id courseId) {
        return (int) enrollments.stream()
                .filter(enrollment -> enrollment.getCourseId().equals(courseId))
                .count();
    }

}
