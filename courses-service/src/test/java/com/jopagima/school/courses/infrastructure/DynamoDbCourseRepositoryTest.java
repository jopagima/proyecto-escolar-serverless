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

import com.jopagima.school.courses.domain.Course;
import com.jopagima.school.courses.domain.CourseAlreadyExistsException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@ExtendWith (MockitoExtension.class)
public class DynamoDbCourseRepositoryTest {

    private static final String TABLE_NAME = "CoursesTable";

    @Mock
    private DynamoDbClient dynamoDbClient; 

    private DynamoDbCourseRepository repository;

    @BeforeEach 
    void setup() {
        repository = new DynamoDbCourseRepository(dynamoDbClient, TABLE_NAME);
    }

    @Test 
    void shouldSaveCourseWithCompositeKeyAndConditionExpression() {
        Course course = Course.create("Advanced Java", 30);

        repository.save(course);

        ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(requestCaptor.capture());

        PutItemRequest request = requestCaptor.getValue();
        assertEquals(TABLE_NAME, request.tableName());
        assertEquals("attribute_not_exists(PK)", request.conditionExpression());

        assertEquals("COURSE#" + course.getId(), request.item().get("PK").s());
        assertEquals("METADATA", request.item().get("SK").s());
        assertEquals("Advanced Java", request.item().get("name").s());
        assertEquals("30", request.item().get("maxCapacity").n());
    }

    @Test
    void shouldTranslateConditionalCheckFailureToDomainException() {
        Course course = Course.create("Advanced Java", 30);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().build());

        assertThrows(CourseAlreadyExistsException.class, () -> repository.save(course));
    }    



}
