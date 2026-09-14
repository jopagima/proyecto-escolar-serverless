package com.jopagima.school.infra.courses;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.assertions.Template;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Design decision (why, not what): courses and course enrollments (Course-Student,
 * N:M) share this single table using the adjacency list pattern — a Course item
 * (PK=COURSE#<id>, SK=METADATA) and its enrollment items (PK=COURSE#<id>,
 * SK=STUDENT#<studentId>) live together, so a single Query on PK returns a course
 * with all its enrolled students. The GSI answers the inverse question ("courses for
 * a student") without a full table scan.
 */

public class CoursesTableConstructTest {

    @Test
    public void shouldCreateOnDemandDynamoDbTableWithCompositeKey() {
        Stack stack = new Stack();
        new CoursesTableConstruct(stack, "CoursesTable");

        Template template = Template.fromStack(stack);
        
        template.hasResourceProperties("AWS::DynamoDB::Table", 
            Map.of(
                "TableName", "CoursesTable",
                "BillingMode", "PAY_PER_REQUEST",
                "KeySchema", List.of(
                    Map.of("AttributeName", "PK", "KeyType", "HASH"),
                    Map.of("AttributeName", "SK", "KeyType", "RANGE")
                ),
                "AttributeDefinitions", List.of(
                    Map.of("AttributeName", "PK", "AttributeType", "S"),
                    Map.of("AttributeName", "SK", "AttributeType", "S"),
                    Map.of("AttributeName", "GSI1PK", "AttributeType", "S"),
                    Map.of("AttributeName", "GSI1SK", "AttributeType", "S")
                ),
                "GlobalSecondaryIndexes", List.of(
                    Map.of(
                        "IndexName", "StudentCoursesIndex",
                        "KeySchema", List.of(
                            Map.of("AttributeName", "GSI1PK", "KeyType", "HASH"),
                            Map.of("AttributeName", "GSI1SK", "KeyType", "RANGE")
                        ),
                        "Projection", Map.of("ProjectionType", "KEYS_ONLY")
                    )
                )
            )
        );

    }
}
