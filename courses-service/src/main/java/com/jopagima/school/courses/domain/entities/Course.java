package com.jopagima.school.courses.domain.entities;

import com.jopagima.school.commons.domain.*;
import com.jopagima.school.courses.domain.valueobjects.CourseCapacity;

/**
 * Course
 */
public class Course {

    private final Id id;
    private final String name;
    private final CourseCapacity maxCapacity;

    private Course(Id id, String name, CourseCapacity maxCapacity) {
        this.id = id;
        this.name = name;
        this.maxCapacity = maxCapacity;
    }

    public static Course create(String name, int maxCapacity) {
        Id id = Id.generateUniqueIdentifier();

        if (name == null || name.trim().isEmpty()) {
            throw ValidationError.create("Course name cannot be blank");
        }
        CourseCapacity capacity = CourseCapacity.create(maxCapacity);
        
        return new Course(id, name, capacity);
    }

    /**
     * Rebuilds a Course already known to exist, from data already trusted (this
     * project's own DynamoDB table) — never used with external/untrusted input. No
     * revalidation: the invariants were already enforced by create() the moment this
     * Course was first persisted.
     */
    public static Course reconstitute(Id id, String name, int maxCapacity) {
        return new Course(id, name, CourseCapacity.reconstitute(maxCapacity));
    }    

    public Id  getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public int getMaxCapacity() {
        return maxCapacity.value();
    }
}
