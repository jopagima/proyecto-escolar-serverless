package com.jopagima.school.infra.students;

import java.util.Map;

import org.junit.jupiter.api.Test;



import software.amazon.awscdk.Stack;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.s3.Bucket;

public class StudentsApiConstructTest {

    @Test
   void shouldCreateLambdaFunctionAndHttpApiRoute(){
        
        Stack stack = new Stack();
        Table table = new StudentsTableConstruct(stack, "StudentsTable").getTable();
        Bucket bucket = new StudentsPhotoBucketConstruct(stack, "StudentsPhotoBucket").getBucket();

        new StudentsApiConstruct(stack, "StudentsApi", table, bucket);
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
        template.hasResourceProperties("AWS::ApiGatewayV2::Route", Map.of(
                "RouteKey", "POST /students"
        ));
   } 
    @Test
    void createsPhotoUploadFunctionAndRoute() {
        Stack stack = new Stack();
        Table table = new StudentsTableConstruct(stack, "StudentsTable").getTable();
        Bucket bucket = new StudentsPhotoBucketConstruct(stack, "StudentsPhotoBucket").getBucket();

        new StudentsApiConstruct(stack, "StudentsApi", table, bucket);

        Template template = Template.fromStack(stack);

        template.hasResourceProperties("AWS::Lambda::Function", Map.of(
                "Handler", "com.jopagima.school.students.infrastructure.RequestStudentPhotoUploadHandler::handleRequest",
                "Environment", Map.of("Variables",
                        Match.objectLike(Map.of("PHOTO_BUCKET_NAME", Match.anyValue())))
        ));
        template.hasResourceProperties("AWS::ApiGatewayV2::Route", Map.of(
                "RouteKey", "GET /students/{studentId}/photo-upload-url"
        ));
    }  

}
