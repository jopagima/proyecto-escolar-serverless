package com.jopagima.school.infra.students;

import java.util.Map;

import org.junit.jupiter.api.Test;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.services.dynamodb.Table;

public class StudentsApiConstructTest {

    @Test
   void shouldCreateLambdaFunctionAndHttpApiRoute(){
        
        Stack stack = new Stack();
        StudentsTableConstruct tableConstruct = new StudentsTableConstruct(stack, "StudentsTable");
        Table table = tableConstruct.getTable();

        new StudentsApiConstruct(stack, "StudentsApi", table);
        Template template = Template.fromStack(stack);

        // Verify that the Lambda function is created with the correct properties
        template.hasResourceProperties("AWS::Lambda::Function", 
            Map.of(
                "Handler", "com.jopagima.school.students.infrastructure.RegisterStudentHandler::handleRequest",
                "Runtime", "java17"
            )
        );

        // Verify that the HTTP API route is created with the correct properties
        template.hasResourceProperties("AWS::ApiGatewayV2::Api", 
            Map.of(
                 "ProtocolType", "HTTP"
            )
        );
        template.resourceCountIs("AWS::ApiGatewayV2::Route", 1);
   } 

}
