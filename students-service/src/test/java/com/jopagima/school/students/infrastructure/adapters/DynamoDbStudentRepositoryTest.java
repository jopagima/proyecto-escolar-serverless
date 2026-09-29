package com.jopagima.school.students.infrastructure.adapters;


import com.jopagima.school.students.domain.entities.Student;
import com.jopagima.school.commons.domain.DomainError;
import com.jopagima.school.commons.domain.ErrorType;

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
   private DynamoDbStudentRepository repository;

  @BeforeEach
  void setUp(){
    repository = new DynamoDbStudentRepository(dynamoDbClient, TABLE_NAME);
  }  

   @Test
   void savesStudentWithCompositeKeyAndConditionExpression(){
    Student student = Student.create("Ana", "Garcia", "ana.garcia@example.com");
    repository.save(student);

    ArgumentCaptor<PutItemRequest> requestCaptor = ArgumentCaptor.forClass(PutItemRequest.class);
    verify(dynamoDbClient).putItem(requestCaptor.capture());
    PutItemRequest request = requestCaptor.getValue();
    assertEquals(TABLE_NAME, request.tableName());
    assertEquals("attribute_not_exists(PK)", request.conditionExpression());
    assertEquals("STUDENT#" + student.getId(), request.item().get("PK").s());
    assertEquals("METADATA", request.item().get("SK").s());
    assertEquals("Ana", request.item().get("firstName").s());
    assertEquals("Garcia", request.item().get("lastName").s());
    assertEquals("ana.garcia@example.com", request.item().get("email").s());
   } 

    @Test
    void translatesConditionalCheckFailureToDomainException() {
        Student student =  Student.create("Ana", "Garcia", "ana.garcia@example.com");
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().build());

        DomainError error = assertThrows(DomainError.class, () -> repository.save(student));
        assertEquals(ErrorType.alreadyExists, error.getType());
    }   
}
