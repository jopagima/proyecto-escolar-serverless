package com.jopagima.school.students.application;

import com.jopagima.school.students.domain.entities.Student;
import com.jopagima.school.students.domain.repositories.StudentRepository;

/**
 * Registers a new Student. Returns the server-generated id as a primitive, so no domain
 * type crosses into the infrastructure layer (guidelinesHexagonal-serverless.md §4.1).
 */
public class RegisterStudentUseCase {

    private final StudentRepository studentRepository;

    public RegisterStudentUseCase(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public String execute(String firstName, String lastName, String email) {
        Student student = Student.create(firstName, lastName, email);
        studentRepository.save(student);
        return student.getId().toString();
    }
}
