package com.jopagima.school.courses.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Course is self-validating: no invalid Course instance should ever exist in memory.
 * maxCapacity is a business invariant (a course cannot have zero or negative seats),
 * not just a format check — distinct from the blank/format checks on id and name.
 */
public class CourseTest {
    @Test 
    void shouldCreateCourseWithValidData() {
        Course course = Course.create("c-001", "Advance Java", 30);

        assertEquals("c-001", course.getId());
        assertEquals("Advance Java", course.getName());
        assertEquals(30, course.getMaxCapacity());
    }
    @Test
    void shouldRejectBlankId() {
        assertThrows(InvalidCourseException.class,
                () -> Course.create(" ", "Advanced Java", 30));
    }  

    @Test
    void shouldRejectBlankName() {
        assertThrows(InvalidCourseException.class,
                () -> Course.create("c-001", " ", 30));
    }    
    
    @Test
    void shouldRejectZeroCapacity() {
        assertThrows(InvalidCourseException.class,
                () -> Course.create("c-001", "Advanced Java", 0));
    }
    
    @Test
    void shouldRejectNegativeCapacity() {
        assertThrows(InvalidCourseException.class,
                () -> Course.create("c-001", "Advanced Java", -5));
    }    

}
