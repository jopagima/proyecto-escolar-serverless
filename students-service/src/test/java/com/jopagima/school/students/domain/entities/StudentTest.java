package com.jopagima.school.students.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jopagima.school.commons.domain.ValidationError;

/**
 * The Student entity must be self-validating: no invalid Student instance should
 * ever exist in memory, regardless of which entry point (Lambda handler, batch
 * import, etc.) creates it.
 */
public class StudentTest {

    private static final String UUID_PATTERN =
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

@Test
    void shouldRejectBlankFirstName() {
        assertThrows(ValidationError.class,
                () -> Student.create(" ", "Garcia", "ana.garcia@example.com"));
    }

    @Test
    void shouldRejectBlankLastName() {
        assertThrows(ValidationError.class,
                () -> Student.create("Ana", "", "ana.garcia@example.com"));
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        assertThrows(ValidationError.class,
                () -> Student.create("Ana", "Garcia", "not-an-email"));
    }

    @Test
    void shouldCreateValidStudentData() {
        Student student = Student.create("John", "Doe", "john.doe@example.com");
        assertNotNull(student.getId());
        assertTrue(student.getId().toString().matches(UUID_PATTERN));
        assertEquals("John", student.getFirstName());
        assertEquals("Doe", student.getLastName());
        assertEquals("john.doe@example.com", student.getEmail());
    }


}
