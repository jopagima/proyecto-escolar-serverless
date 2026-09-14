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

    /**
     * Verifies that the construct synthesizes a single DynamoDB table wired for the
     * adjacency list pattern described above: on-demand billing, the PK/SK composite
     * primary key that stores both Course and Course-Student items, and the
     * StudentCoursesIndex GSI that lets us query "courses for a student" without a
     * table scan. This is a CDK synthesis test (via Template.fromStack), not a
     * runtime/integration test against a real DynamoDB table.
     */
    @Test
    public void shouldCreateOnDemandDynamoDbTableWithCompositeKey() {
        // Synthesize the stack containing the construct under test.
        Stack stack = new Stack();
        new CoursesTableConstruct(stack, "CoursesTable");

        Template template = Template.fromStack(stack);

        // Assert the synthesized CloudFormation template contains a DynamoDB table
        // resource with exactly the properties the adjacency list design relies on.
        template.hasResourceProperties("AWS::DynamoDB::Table",
            Map.of(
                "TableName", "CoursesTable",
                // On-demand capacity: no need to provision/manage throughput for this table.
                "BillingMode", "PAY_PER_REQUEST",
                // Composite primary key (PK/SK) enabling both Course (PK+METADATA) and
                // Course-Student enrollment (PK+STUDENT#id) items in the same table.
                "KeySchema", List.of(
                    Map.of("AttributeName", "PK", "KeyType", "HASH"),
                    Map.of("AttributeName", "SK", "KeyType", "RANGE")
                ),
                // All four attributes used as keys (table's own PK/SK plus the GSI's
                // GSI1PK/GSI1SK) must be declared here for DynamoDB to accept them.
                "AttributeDefinitions", List.of(
                    Map.of("AttributeName", "PK", "AttributeType", "S"),
                    Map.of("AttributeName", "SK", "AttributeType", "S"),
                    Map.of("AttributeName", "GSI1PK", "AttributeType", "S"),
                    Map.of("AttributeName", "GSI1SK", "AttributeType", "S")
                ),
                // Inverse-lookup GSI: answers "which courses is this student enrolled in"
                // without scanning the table. KEYS_ONLY projection keeps it lean since the
                // main table query already returns full item data.
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
