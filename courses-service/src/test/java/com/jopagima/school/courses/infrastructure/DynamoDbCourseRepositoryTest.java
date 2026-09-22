package com.jopagima.school.courses.infrastructure;


import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.courses.domain.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jopagima.school.courses.domain.CourseAlreadyExistsException;

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

    @Test
    void findsCourseByIdWhenItExists() {
        Id id = Id.generateUniqueIdentifier();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(
                GetItemResponse.builder().item(Map.of(
                        "PK", AttributeValue.builder().s("COURSE#" + id).build(),
                        "SK", AttributeValue.builder().s("METADATA").build(),
                        "id", AttributeValue.builder().s(id.toString()).build(),
                        "name", AttributeValue.builder().s("Advanced Java").build(),
                        "maxCapacity", AttributeValue.builder().n("30").build()
                )).build());

        Optional<Course> found = repository.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("Advanced Java", found.get().getName());
        assertEquals(30, found.get().getMaxCapacity());
    }

    @Test
    void returnsEmptyWhenCourseDoesNotExist() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder().build());

        Optional<Course> found = repository.findById(Id.generateUniqueIdentifier());

        assertTrue(found.isEmpty());
    }
}
