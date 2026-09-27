package com.jopagima.school.courses.application;


import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.courses.domain.*;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
public class EnrollStudentInCourseUseCaseTest {
    @Test
    void enrollsStudentWhenCourseHasCapacity() {
        Course course = Course.create("Advanced Java", 30);
        CourseRepository courseRepository = new InMemoryCourseRepository();
        courseRepository.save(course);
        CourseEnrollmentRepository enrollmentRepository = new InMemoryCourseEnrollmentRepository();

        EnrollStudentInCourseUseCase useCase =
                new EnrollStudentInCourseUseCase(courseRepository, enrollmentRepository);

        Id studentId = Id.generateUniqueIdentifier();

        useCase.execute(course.getId().toString(), studentId.toString());

        assertEquals(1, enrollmentRepository.countEnrollments(course.getId()));
    } 
    
    @Test
    void doesNotAllowEnrollmentWhenCourseDoesNotExist() {
        CourseRepository courseRepository = new InMemoryCourseRepository();
        CourseEnrollmentRepository enrollmentRepository = new InMemoryCourseEnrollmentRepository();
        Id studentId = Id.generateUniqueIdentifier();
        
        EnrollStudentInCourseUseCase useCase =
                new EnrollStudentInCourseUseCase(courseRepository, enrollmentRepository);

        assertThrows(InvalidCourseException.class, () -> useCase.execute(
                Id.generateUniqueIdentifier().toString(), studentId.toString()));
    }

    @Test
    void doesNotAllowEnrollmentWhenCourseIsAtFullCapacity() {
        Course course = Course.create("Advanced Java", 1);
        CourseRepository courseRepository = new InMemoryCourseRepository();
        courseRepository.save(course);
        CourseEnrollmentRepository enrollmentRepository = new InMemoryCourseEnrollmentRepository();
        Id studentId = Id.generateUniqueIdentifier();
        enrollmentRepository.enroll(CourseEnrollment.create(course.getId(), studentId));

        EnrollStudentInCourseUseCase useCase =
                new EnrollStudentInCourseUseCase(courseRepository, enrollmentRepository);

        assertThrows(InvalidCourseEnrollmentException.class, () -> useCase.execute(course.getId().toString(), studentId.toString()));
    }    

}
