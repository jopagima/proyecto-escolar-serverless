package com.jopagima.school.students.domain.repositories;

import com.jopagima.school.students.domain.entities.Student;

/**
 * Port: the domain depends on this interface, never on a concrete AWS SDK type.
 * Implemented by an infrastructure adapter (see DynamoDbStudentRepository).
 */
public interface StudentRepository {
     void save(Student student);
}
