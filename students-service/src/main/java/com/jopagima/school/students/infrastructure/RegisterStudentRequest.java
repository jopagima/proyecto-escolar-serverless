package com.jopagima.school.students.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Inbound request DTO for the HTTP boundary. Kept separate from the Student domain
 * entity so that JSON deserialization concerns never leak into the domain layer.
 * The student id is generated server-side: a client-supplied "id" is ignored.
 */
@JsonIgnoreProperties({"id"})
public class RegisterStudentRequest {
    private String firstName;
    private String lastName;
    private String email;

    public RegisterStudentRequest() {
    }


    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }


    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    public void setEmail(String email) {
        this.email = email;
    }


    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
}
