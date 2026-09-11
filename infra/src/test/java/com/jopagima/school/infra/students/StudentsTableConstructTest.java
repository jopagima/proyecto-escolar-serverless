package com.jopagima.school.infra.students;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;
import software.amazon.awscdk.services.dynamodb.Table;

public class StudentsTableConstructTest {
    /**
 * Design decision (why, not what): the students table uses on-demand billing
 * (PAY_PER_REQUEST) to avoid the fixed-cost antipattern for a low, irregular-traffic
 * PoC workload. PK/SK overloading is used from day one as a project-wide convention,
 * even though the Student domain currently has a single item type.
 */

    @Test 
    void shouldCreateOnDemandDynamoDbTableWithCompositeKey (){
         // TODO 1: create a bare `Stack` instance to host the construct under test.
         Stack stack = new Stack();


                // Define the Lambda function
        StudentsTableConstruct studentsTableConstruct = new StudentsTableConstruct(stack, "StudentsTable");

         // TODO 2: instantiate StudentsTableConstruct(stack, "StudentsTable")
        //         once the production class exists.
        Template template = Template.fromStack(stack); 

        template.hasResourceProperties("AWS::DynamoDB::Table", 
            Map.of(
                "TableName", "StudentsTable",
                "BillingMode", "PAY_PER_REQUEST",
                "KeySchema", Match.arrayWith(List.of(
                    Map.of("AttributeName", "PK", "KeyType", "HASH"),
                    Map.of("AttributeName", "SK", "KeyType", "RANGE")
                )),
                "AttributeDefinitions", Match.arrayWith(List.of(
                    Map.of("AttributeName", "PK", "AttributeType", "S"),
                        Map.of("AttributeName", "SK", "AttributeType", "S")
                ))
            )
        );


    }
    
 
  

}
