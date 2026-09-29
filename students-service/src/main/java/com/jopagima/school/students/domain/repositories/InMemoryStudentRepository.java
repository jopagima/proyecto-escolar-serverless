package com.jopagima.school.students.domain.repositories;

import com.jopagima.school.commons.domain.DomainError;
import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.students.domain.entities.Student;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryStudentRepository implements StudentRepository {

    private final Map<Id, Student> students = new HashMap<>();

    /**
     * Mirrors the DynamoDB adapter's attribute_not_exists(PK) condition.
     */
    @Override
    public void save(Student student) {
        if (students.containsKey(student.getId())) {
            throw DomainError.createAlreadyExists("Student already exists: " + student.getId());
        }
        students.put(student.getId(), student);
    }

    /**
     * Test-side lookup only — not part of the StudentRepository port.
     */
    public Optional<Student> findById(Id id) {
        return Optional.ofNullable(students.get(id));
    }
}
