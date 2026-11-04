package com.jopagima.school.infra.students;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    @Test
    void everyAlarmWatchesTheLambdaErrorsMetric() {
        Map<String, Map<String, Object>> alarms = template.findResources("AWS::CloudWatch::Alarm");

        assertEquals(2, alarms.size());
        alarms.values().forEach(alarm -> {
            Map<?, ?> properties = (Map<?, ?>) alarm.get("Properties");
            assertEquals("Errors", properties.get("MetricName"));
            assertEquals("AWS/Lambda", properties.get("Namespace"));
        });
    }  

    @Test
    void everyAlarmWatchesADifferentLambda() {
        Map<String, Map<String, Object>> alarms = template.findResources("AWS::CloudWatch::Alarm");

        Set<Object> watchedFunctions = new HashSet<>();
        alarms.values().forEach(alarm -> {
            Map<?, ?> properties = (Map<?, ?>) alarm.get("Properties");
            List<?> dimensions = (List<?>) properties.get("Dimensions");
            dimensions.stream()
                    .map(dimension -> (Map<?, ?>) dimension)
                    .filter(dimension -> "FunctionName".equals(dimension.get("Name")))
                    .forEach(dimension -> watchedFunctions.add(dimension.get("Value")));
        });

        assertEquals(2, watchedFunctions.size());
    }
    
    @Test
    void notifiesTheOperationalAlertsTopicFromEveryAlarm() {
        template.resourceCountIs("AWS::SNS::Topic", 1);
        template.findResources("AWS::CloudWatch::Alarm").values().forEach(alarm ->
                assertNotNull(((Map<?, ?>) alarm.get("Properties")).get("AlarmActions")));
    }
}
