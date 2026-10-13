package com.jopagima.school.infra.students;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.assertions.Template;

public class StudentsUserPoolConstructTest {

    private Template template;

    @BeforeEach 
    void setUp() {
        Stack stack = new Stack();
        new StudentsUserPoolConstruct(stack, "StudentsUserPool");
        template = Template.fromStack(stack);
    }

    @Test 
    void createUserPoolWhereOnlyAdminsCreatesUsers() {
        template.hasResourceProperties("AWS::Cognito::UserPool", Map.of(
            "UsernameAttributes", List.of("email"),
            "AdminCreateUserConfig", Map.of("AllowAdminCreateUserOnly", true)));
    }

    @Test
    void createsStudentTeacherAndAdminGroups() {
        template.resourceCountIs("AWS::Cognito::UserPoolGroup", 3);
        for (String groupName : List.of("student", "teacher", "admin")) {
            template.hasResourceProperties("AWS::Cognito::UserPoolGroup",
                    Map.of("GroupName", groupName));
        }
    }    
    

}
