package com.jopagima.school.courses.domain.services;

import com.jopagima.school.courses.infrastructure.DynamoDbCourseEnrollmentRepository;

/**
 * Domain Service (see guidelinesHexagonal-serverless.md §4.4): stateless pure function
 * operating on values from two different aggregates (Course's capacity, and the current
 * enrollment count computed by CourseEnrollmentRepository). Neither Course nor
 * CourseEnrollment individually has enough information to answer this question.
 */
public class EnrollmentEligibilityService {


    private EnrollmentEligibilityService() {
     // Prevent instantiation — this class is a namespace for static functions.
    }
    
    public static boolean canEnroll(int currentEnrollmentCount, int maxCapacity) {
        // TODO 3: return true only if currentEnrollmentCount is strictly less than
        //   maxCapacity. One line.
        return currentEnrollmentCount < maxCapacity;
    }
}    
