package com.jopagima.school.infra.students;

import org.junit.jupiter.api.Test;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Template;

public class StudentsStackTest {
    
    @Test
    void deploysTheOperationalAlarmsAndTheirTopic() {
        App app = new App();
        StudentsStack stack = new StudentsStack(app, "TestStudentsStack", StackProps.builder().build());
        Template template = Template.fromStack(stack);
        template.resourceCountIs("AWS::CloudWatch::Alarm", 2);
        template.resourceCountIs("AWS::SNS::Topic", 1);
    }
}
