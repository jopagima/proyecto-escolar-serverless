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

    public void execute(Id courseId, String studentId) {
        // Implementation of the use case to enroll a student in a course
        // This would typically involve checking if the course exists,
        // if the student is already enrolled, and if the course has capacity.

        


        // TODO 10: find the course via courseRepository.findById(id); if empty, throw
        //   DomainError.createNotFound("Course " + courseId + " not found").

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new InvalidCourseException("Course " + courseId + " not found"));

        
        // TODO 11: count current enrollments via
        //   courseEnrollmentRepository.countEnrollments(id).
        int currentEnrollments = courseEnrollmentRepository.countEnrollments(courseId);

        // TODO 12: ask EnrollmentEligibilityService.canEnroll(currentCount,
        //   course.getMaxCapacity()); if false, throw
        //   DomainError.create("Course " + courseId + " is at full capacity").

        if(course.getMaxCapacity() <= currentEnrollments) {
            throw new InvalidCourseEnrollmentException("Course " + courseId + " is at full capacity");
        }
        

        // TODO 13: build new CourseEnrollment(id, Id.generateFromPlainTextIdentifier(studentId))
        //   and persist it via courseEnrollmentRepository.enroll(...).

        CourseEnrollment courseEnrollment =  CourseEnrollment.create(courseId, studentId);
        courseEnrollmentRepository.enroll(courseEnrollment);
    }
}
