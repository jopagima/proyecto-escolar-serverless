package com.jopagima.school.students.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.jopagima.school.commons.domain.ValidationError;

/**
 * The Student entity must be self-validating: no invalid Student instance should
 * ever exist in memory, regardless of which entry point (Lambda handler, batch
 * import, etc.) creates it.
 */
public class StudentTest {

@Test
    void shouldRejectBlankFirstName() {
        assertThrows(ValidationError.class,
                () -> Student.create("s-001", " ", "Garcia", "ana.garcia@example.com"));
    }

    @Test
    void shouldRejectBlankLastName() {
        assertThrows(ValidationError.class,
                () -> Student.create("s-001", "Ana", "", "ana.garcia@example.com"));
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        assertThrows(ValidationError.class,
                () -> Student.create("s-001", "Ana", "Garcia", "not-an-email"));
    }

    @Test
    void shouldRejectBlankId() {
        assertThrows(ValidationError.class,
                () -> Student.create("", "Ana", "Garcia", "ana.garcia@example.com"));
    }    

    @Test
    void shouldCreateValidStudentData() {
        Student student = Student.create("STUDENT#123", "John", "Doe", "john.doe@example.com");
        assertEquals("STUDENT#123", student.getId());
        assertEquals("John", student.getFirstName());
        assertEquals("Doe", student.getLastName());
        assertEquals("john.doe@example.com", student.getEmail());
    }


}
