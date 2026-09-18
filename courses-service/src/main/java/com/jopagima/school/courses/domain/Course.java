package com.jopagima.school.courses.domain;

/**
 * Course
 */
public class Course {

    private final String id;
    private final String name;
    private final int maxCapacity;

    private Course(String id, String name, int maxCapacity) {
        this.id = id;
        this.name = name;
        this.maxCapacity = maxCapacity;
    }

    public static Course create(String id, String name, int maxCapacity) {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidCourseException("Course id cannot be blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidCourseException("Course name cannot be blank");
        }
        if (maxCapacity <= 0) {
            throw new InvalidCourseException("Course max capacity must be greater than zero");
        }
        
        return new Course(id, name, maxCapacity);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}
