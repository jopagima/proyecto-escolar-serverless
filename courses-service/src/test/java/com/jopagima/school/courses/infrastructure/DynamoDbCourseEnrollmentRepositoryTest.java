package com.jopagima.school.courses.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jopagima.school.courses.domain.CourseEnrollment;
import com.jopagima.school.courses.domain.EnrollmentAlreadyExistsException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;

@ExtendWith (MockitoExtension.class)
public class DynamoDbCourseEnrollmentRepositoryTest {
    private static final String TABLE_NAME = "CoursesTable";

    @Mock 
    private DynamoDbClient dynamoDbClient;

    private DynamoDbCourseEnrollmentRepository repository;

    @BeforeEach 
    void setUp() {
        repository = new DynamoDbCourseEnrollmentRepository(dynamoDbClient, TABLE_NAME);
    }

    @Test 
    void savesEnrollmentWithCompositeKeyAndConditionExpression() {
        CourseEnrollment enrollment =  CourseEnrollment.create("c-001", "s-001");

        repository.enroll(enrollment);

        ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(requestCaptor.capture());

        PutItemRequest request = requestCaptor.getValue();
        assertEquals(TABLE_NAME, request.tableName());
        assertEquals("attribute_not_exists(PK)", request.conditionExpression());
        assertEquals("COURSE#c-001", request.item().get("PK").s());
        assertEquals("STUDENT#s-001", request.item().get("SK").s());
    }    

    @Test
    void translatesConditionalCheckFailureToDomainException() {
        CourseEnrollment enrollment =  CourseEnrollment.create("c-001", "s-001");
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().build());

        assertThrows(EnrollmentAlreadyExistsException.class, () -> repository.enroll(enrollment));
    }
    
    @Test
    void countsEnrollmentsUsingAdjacencyListQuery() {
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(QueryResponse.builder().count(12).build());

        int count = repository.countEnrollments("c-001");

        assertEquals(12, count);

        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(dynamoDbClient).query(requestCaptor.capture());

        QueryRequest request = requestCaptor.getValue();
        assertEquals(TABLE_NAME, request.tableName());
        assertEquals(Select.COUNT, request.select());
        assertEquals("COURSE#c-001", request.expressionAttributeValues().get(":pk").s());
        assertEquals("STUDENT#", request.expressionAttributeValues().get(":skPrefix").s());
    }    
}
