package com.jopagima.school.students.infrastructure;

import java.util.Map;

import com.jopagima.school.students.domain.Student;
import com.jopagima.school.students.domain.StudentRepository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;


/**
 * Infrastructure adapter implementing the StudentRepository port with the AWS SDK v2
 * low-level DynamoDbClient. Item mapping is done explicitly (no Enhanced Client
 * annotations) to keep the domain (Student) free of any AWS-specific dependency.
 */

public class DynamoDBStudentRepository implements StudentRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDBStudentRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @Override
    public void save(Student student) {
        // TODO 5: build the item Map<String, AttributeValue> with:
        //   PK = "STUDENT#" + student.getId(), SK = "METADATA",
        //   firstName, lastName, email as string attributes.

        Map<String, AttributeValue> item = Map.of("PK", AttributeValue.builder().s("STUDENT#" + student.getId()).build(),
                "SK", AttributeValue.builder().s("METADATA").build(),
                "firstName", AttributeValue.builder().s(student.getFirstName()).build(),
                "lastName", AttributeValue.builder().s(student.getLastName()).build(),
                "email", AttributeValue.builder().s(student.getEmail()).build());

        // TODO 6: build a PutItemRequest with tableName, the item above, and
        //   conditionExpression("attribute_not_exists(PK)").
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .conditionExpression("attribute_not_exists(PK)")
                .build();

        // TODO 7: call dynamoDbClient.putItem(request), catching
        //   ConditionalCheckFailedException and rethrowing as
        //   StudentAlreadyExistsException(student.getId()).
        dynamoDbClient.putItem(request);
    }
}
