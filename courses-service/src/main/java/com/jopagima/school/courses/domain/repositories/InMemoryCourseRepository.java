package com.jopagima.school.courses.domain.repositories;

import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.courses.domain.entities.Course;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryCourseRepository implements CourseRepository{

    private final Map<Id, Course> courses = new HashMap<>();

    @Override
    public void save(Course course) {
        courses.put(course.getId(), course);
    }

    @Override
    public Optional<Course> findById(Id id) {
        return Optional.ofNullable(courses.get(id));
    }

}
