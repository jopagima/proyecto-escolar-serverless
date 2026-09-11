package com.jopagima.school.students.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.jopagima.school.students.domain.Student;
import com.jopagima.school.students.domain.StudentAlreadyExistsException;
import com.jopagima.school.students.domain.StudentRepository;

@ExtendWith(MockitoExtension.class)
public class RegisterStudentHandlerTest {

    @Mock
    private StudentRepository studentRepository;

    private RegisterStudentHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new RegisterStudentHandler(studentRepository);
    }

    @Test
    void shouldReturn201WhenIsRegisteredSuccessfully() {
        APIGatewayV2HTTPEvent event = eventWithBody("{\"id\":\"s-001\",\"firstName\":\"Ana\",\"lastName\":\"Garcia\",\"email\":\"ana.garcia@example.com\"}");
        // Implement the test logic here
        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
        assertEquals(201, response.getStatusCode());    
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertEquals("s-001", studentCaptor.getValue().getId());
    }

    @Test
    void shouldReturn400WhenDomainValidationFails() {
        APIGatewayV2HTTPEvent event = eventWithBody(
                "{\"id\":\"s-001\",\"firstName\":\"\",\"lastName\":\"Garcia\",\"email\":\"ana.garcia@example.com\"}");

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("firstName"));
    }

    @Test
    void shouldReturn409WhenStudentAlreadyExists() {
        APIGatewayV2HTTPEvent event = eventWithBody(
                "{\"id\":\"s-001\",\"firstName\":\"Ana\",\"lastName\":\"Garcia\",\"email\":\"ana.garcia@example.com\"}");
        doThrow(new StudentAlreadyExistsException("s-001"))
                .when(studentRepository).save(any(Student.class));

        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(409, response.getStatusCode());
    }

       

    private APIGatewayV2HTTPEvent eventWithBody(String body) {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setBody(body);
        return event;
    }
    

}
