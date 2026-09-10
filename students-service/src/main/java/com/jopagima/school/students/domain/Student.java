package com.jopagima.school.students.domain;

import java.util.regex.Pattern;

public class Student {

        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
        private final String id;
        private final String firstName;
        private final String lastName;
        private final String email;

        private Student(String id, String firstName, String lastName, String email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        public static Student create(String id, String firstName, String lastName, String email) {
            validate(id, firstName, lastName, email);
            return new Student(id, firstName, lastName, email);
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
            return email;
        }

        private static void validate(String id, String firstName, String lastName, String email) {
            if (id == null || id.isBlank()) {
                throw new InvalidStudentException("Student id cannot be blank");
            }
            if (firstName == null || firstName.isBlank()) {
                throw new InvalidStudentException("Student first name cannot be blank");
            }
            if (lastName == null || lastName.isBlank()) {
                throw new InvalidStudentException("Student last name cannot be blank");
            }
            if (email == null || email.isBlank()) {
                throw new InvalidStudentException("Student email cannot be blank");
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new InvalidStudentException("Student email must be valid");
            }
        }
}
