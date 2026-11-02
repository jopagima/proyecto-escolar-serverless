package com.jopagima.school.infra.students;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;


public class StudentsAlertsConstructTest {
    private Template template;
    

    @BeforeEach
    void setUp() {
        Stack stack = new Stack();
        Function registerStudent = lambdaNamed(stack, "RegisterStudentFunction");
        Function photoUpload = lambdaNamed(stack, "PhotoUploadFunction");
        new StudentsAlertsConstruct(stack, "StudentsAlerts", registerStudent, photoUpload);
        template = Template.fromStack(stack);
    }


    private static Function lambdaNamed(Stack stack, String id) {
        return Function.Builder.create(stack, id)
            .runtime(Runtime.NODEJS_24_X)
            .handler("index.handler")
            .code(Code.fromInline("exports.handler = async () => {};"))
            .build();
    }


    @Test
    void createsOneAlarmPerLambdaOnTheErrorsMetric() {
        template.resourceCountIs("AWS::CloudWatch::Alarm", 2);
        template.hasResourceProperties("AWS::CloudWatch::Alarm", Map.of(
                "MetricName", "Errors",
                "Namespace", "AWS/Lambda"));
    }     
}
