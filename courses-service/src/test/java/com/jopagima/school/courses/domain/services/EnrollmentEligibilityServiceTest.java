package com.jopagima.school.courses.domain.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pure domain logic: whether a course can accept one more enrollment. Does not belong
 * to Course (which knows its own capacity but not the current enrollment count coming
 * from a separate aggregate) nor to CourseEnrollment (which knows neither).
 */
public class EnrollmentEligibilityServiceTest {

    @Test 
    void allowsEnrollmentWhenBelowCapacity() {
        boolean eligible = EnrollmentEligibilityService.canEnroll(29, 30);

        assertTrue(eligible);
    }

    @Test
    void doesNotAllowEnrollmentWhenAtCapacity() {
        boolean eligible = EnrollmentEligibilityService.canEnroll(30, 30);

        assertFalse(eligible);
    }

    @Test
    void doesNotAllowEnrollmentWhenAboveCapacity() {
        boolean eligible = EnrollmentEligibilityService.canEnroll(31, 30);

        assertFalse(eligible);
    }
}
