package com.jopagima.school.courses.application;

import com.jopagima.school.courses.domain.entities.Course;
import com.jopagima.school.courses.domain.entities.CourseEnrollment;
import com.jopagima.school.courses.domain.repositories.CourseEnrollmentRepository;
import com.jopagima.school.courses.domain.repositories.CourseRepository;
import com.jopagima.school.courses.domain.services.EnrollmentEligibilityService;
import com.jopagima.school.commons.domain.DomainError;
import com.jopagima.school.commons.domain.Id;

public class EnrollStudentInCourseUseCase {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    

    
    public EnrollStudentInCourseUseCase(CourseRepository courseRepository, CourseEnrollmentRepository courseEnrollmentRepository) {
        this.courseRepository = courseRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    public void execute(String courseId, String studentId) {

        Id courseIdentifier  = Id.generateFromPlainTextIdentifier(courseId);
        Course course = courseRepository.findById(courseIdentifier).orElseThrow(() -> DomainError.createNotFound("Course " + courseId + " not found"));
        int currentEnrollments = courseEnrollmentRepository.countEnrollments(courseIdentifier);
        if (!EnrollmentEligibilityService.canEnroll(currentEnrollments, course.getMaxCapacity())) {
            throw DomainError.create("Course " + courseId + " is at full capacity");
        }
        
        Id studentIdentifier = Id.generateFromPlainTextIdentifier(studentId);
        CourseEnrollment courseEnrollment =  CourseEnrollment.create(courseIdentifier, studentIdentifier);
        courseEnrollmentRepository.enroll(courseEnrollment);
    }
}
