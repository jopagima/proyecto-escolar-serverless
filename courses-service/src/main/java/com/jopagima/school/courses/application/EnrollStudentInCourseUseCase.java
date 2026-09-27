package com.jopagima.school.courses.application;

import com.jopagima.school.courses.domain.Course;
import com.jopagima.school.courses.domain.CourseEnrollment;
import com.jopagima.school.courses.domain.CourseEnrollmentRepository;
import com.jopagima.school.courses.domain.CourseRepository;
import com.jopagima.school.courses.domain.InvalidCourseEnrollmentException;
import com.jopagima.school.courses.domain.InvalidCourseException;
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
        Course course = courseRepository.findById(courseIdentifier).orElseThrow(() -> new InvalidCourseException("Course " + courseId + " not found"));
        int currentEnrollments = courseEnrollmentRepository.countEnrollments(courseIdentifier);
        if(course.getMaxCapacity() <= currentEnrollments) {
            throw new InvalidCourseEnrollmentException("Course " + courseId + " is at full capacity");
        }
        
        Id studentIdentifier = Id.generateFromPlainTextIdentifier(studentId);
        CourseEnrollment courseEnrollment =  CourseEnrollment.create(courseIdentifier, studentIdentifier);
        courseEnrollmentRepository.enroll(courseEnrollment);
    }
}
