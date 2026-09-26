package com.jopagima.school.courses.application;


import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.commons.domain.DomainError;
import com.jopagima.school.courses.domain.*;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
public class EnrollStudentInCourseUseCaseTest {
    @Test
    void enrollsStudentWhenCourseHasCapacity() {
        Course course = Course.create("Advanced Java", 30);
        InMemoryCourseRepository courseRepository = new InMemoryCourseRepository();
        courseRepository.save(course);
        InMemoryCourseEnrollmentRepository enrollmentRepository = new InMemoryCourseEnrollmentRepository();

        EnrollStudentInCourseUseCase useCase =
                new EnrollStudentInCourseUseCase(courseRepository, enrollmentRepository);

        String studentId = "st-12345";

        useCase.execute(course.getId(), studentId);

        assertEquals(1, enrollmentRepository.countEnrollments(course.getId()));
    } 
    
    @Test
    void doesNotAllowEnrollmentWhenCourseDoesNotExist() {
        CourseRepository courseRepository = new InMemoryCourseRepository();
        CourseEnrollmentRepository enrollmentRepository = new InMemoryCourseEnrollmentRepository();

        EnrollStudentInCourseUseCase useCase =
                new EnrollStudentInCourseUseCase(courseRepository, enrollmentRepository);

        assertThrows(InvalidCourseException.class, () -> useCase.execute(
                Id.generateUniqueIdentifier(), "s-001"));
    }

}
