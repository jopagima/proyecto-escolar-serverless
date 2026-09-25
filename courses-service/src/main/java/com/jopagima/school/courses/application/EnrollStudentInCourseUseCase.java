package com.jopagima.school.courses.application;

import com.jopagima.school.courses.domain.CourseEnrollment;
import com.jopagima.school.courses.domain.CourseEnrollmentRepository;
import com.jopagima.school.courses.domain.CourseRepository;
import com.jopagima.school.commons.domain.Id;

public class EnrollStudentInCourseUseCase {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    

    
    public EnrollStudentInCourseUseCase(CourseRepository courseRepository, CourseEnrollmentRepository courseEnrollmentRepository) {
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    public void execute(Id courseId, String studentId) {
        // Implementation of the use case to enroll a student in a course
        // This would typically involve checking if the course exists,
        // if the student is already enrolled, and if the course has capacity.

        CourseEnrollment courseEnrollment =  CourseEnrollment.create(courseId, studentId);
        courseEnrollmentRepository.enroll(courseEnrollment);

        // TODO 10: find the course via courseRepository.findById(id); if empty, throw
        //   DomainError.createNotFound("Course " + courseId + " not found").

        // TODO 11: count current enrollments via
        //   courseEnrollmentRepository.countEnrollments(id).

        // TODO 12: ask EnrollmentEligibilityService.canEnroll(currentCount,
        //   course.getMaxCapacity()); if false, throw
        //   DomainError.create("Course " + courseId + " is at full capacity").

        // TODO 13: build new CourseEnrollment(id, Id.generateFromPlainTextIdentifier(studentId))
        //   and persist it via courseEnrollmentRepository.enroll(...).
    }
}
