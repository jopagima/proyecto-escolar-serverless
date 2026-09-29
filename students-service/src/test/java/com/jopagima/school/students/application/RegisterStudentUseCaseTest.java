package com.jopagima.school.students.application;

import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.commons.domain.ValidationError;
import com.jopagima.school.students.domain.entities.Student;
import com.jopagima.school.students.domain.repositories.InMemoryStudentRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegisterStudentUseCaseTest {

    @Test
    void registersStudentUnderServerGeneratedId() {
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        RegisterStudentUseCase useCase = new RegisterStudentUseCase(studentRepository);

        String studentId = useCase.execute("Ana", "Garcia", "ana.garcia@example.com");

        Student student = studentRepository.findById(Id.generateFromPlainTextIdentifier(studentId)).orElseThrow();
        assertEquals("Ana", student.getFirstName());
        assertEquals("Garcia", student.getLastName());
        assertEquals("ana.garcia@example.com", student.getEmail());
    }

    @Test
    void doesNotRegisterStudentWithInvalidEmail() {
        InMemoryStudentRepository studentRepository = new InMemoryStudentRepository();
        RegisterStudentUseCase useCase = new RegisterStudentUseCase(studentRepository);

        assertThrows(ValidationError.class, () -> useCase.execute("Ana", "Garcia", "not-an-email"));
    }
}
