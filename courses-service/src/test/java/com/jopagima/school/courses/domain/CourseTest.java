package com.jopagima.school.courses.domain;

import static org.junit.jupiter.api.Assertions.*;


import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import com.jopagima.school.commons.domain.*;

/**
 * Course is self-validating: no invalid Course instance should ever exist in memory.
 * maxCapacity is a business invariant (a course cannot have zero or negative seats),
 * not just a format check — distinct from the blank/format checks on id and name.
 */
public class CourseTest {
     private static final String UUID_PATTERN = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}";
    @Test 
    void shouldCreateCourseWithValidData() {

        
        Course course = Course.create("Advance Java", 30);

        Id id =  course.getId();
        assertTrue(Pattern.compile(UUID_PATTERN).matcher(id.toString()).matches());
        assertEquals("Advance Java", course.getName());
        assertEquals(30, course.getMaxCapacity());
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(InvalidCourseException.class,
                () -> Course.create( " ", 30));
    }    
    
    @Test
    void shouldRejectZeroCapacity() {
        assertThrows(InvalidCourseException.class,
                () -> Course.create("Advanced Java", 0));
    }
    
    @Test
    void shouldRejectNegativeCapacity() {
        assertThrows(InvalidCourseException.class,
                () -> Course.create("Advanced Java", -5));
    }    

}
