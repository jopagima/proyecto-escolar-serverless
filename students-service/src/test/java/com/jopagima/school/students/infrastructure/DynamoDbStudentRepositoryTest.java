package com.jopagima.school.students.infrastructure;


import com.jopagima.school.students.domain.Student;
import com.jopagima.school.students.domain.StudentAlreadyExistsException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DynamoDbStudentRepositoryTest {
    private static final String TABLE_NAME = "StudentsTable";

   @Mock
   private DynamoDbClient  dynamoDbClient;
   private DynamoDBStudentRepository repository;

  @BeforeEach
  void setUp(){
    repository = new DynamoDBStudentRepository(dynamoDbClient, TABLE_NAME);
  }  

   @Test
   void shouldSaveStudentWithCompositeKeyAndConditionExpression(){
    Student student = Student.create("s-001", "Ana", "Garcia", "ana.garcia@example.com");
    repository.save(student);

    ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
    verify(dynamoDbClient).putItem(requestCaptor.capture());
    PutItemRequest request = requestCaptor.getValue();
    assertEquals(TABLE_NAME, request.tableName());
    assertEquals("attribute_not_exists(PK)", request.conditionExpression());
    assertEquals("STUDENT#s-001", request.item().get("PK").s());
    assertEquals("METADATA", request.item().get("SK").s());
    assertEquals("Ana", request.item().get("firstName").s());
    assertEquals("Garcia", request.item().get("lastName").s());
    assertEquals("ana.garcia@example.com", request.item().get("email").s());
   } 

    @Test
    void shouldTranslateConditionalCheckFailureToDomainException() {
        Student student =  Student.create("s-001", "Ana", "Garcia", "ana.garcia@example.com");
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().build());

        assertThrows(StudentAlreadyExistsException.class, () -> repository.save(student));
    }   
}
