package com.jopagima.school.students.domain;


public class StudentAlreadyExistsException extends RuntimeException {
    
    public StudentAlreadyExistsException(String studentId) {
        super("Student already exists: " + studentId);
    }

}
