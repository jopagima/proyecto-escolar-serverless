package com.jopagima.school.students.domain.entities;

import com.jopagima.school.commons.domain.ValidationError;
import com.jopagima.school.students.domain.valueobjects.Email;

public class Student {

        private final String id;
        private final String firstName;
        private final String lastName;
        private final Email email;

        private Student(String id, String firstName, String lastName, Email email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        public static Student create(String id, String firstName, String lastName, String email) {
            validate(id, firstName, lastName);
            return new Student(id, firstName, lastName, Email.create(email));
        }

        public String getId() {
            return id;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getEmail() {
            return email.value();
        }

        private static void validate(String id, String firstName, String lastName) {
            if (id == null || id.isBlank()) {
                throw ValidationError.create("id cannot be blank");
            }
            if (firstName == null || firstName.isBlank()) {
                throw ValidationError.create("firstName cannot be blank");
            }
            if (lastName == null || lastName.isBlank()) {
                throw ValidationError.create("lastName cannot be blank");
            }
        }
}
