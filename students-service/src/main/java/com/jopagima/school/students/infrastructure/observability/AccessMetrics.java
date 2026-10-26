package com.jopagima.school.students.infrastructure.observability;

/**
 * AccessMetrics
 */
public interface AccessMetrics {

    void recordDenied(String operation, DenialReason reason);

}
