package com.jopagima.school.students.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jopagima.school.students.domain.InvalidStudentException;
import com.jopagima.school.students.domain.Student;
import com.jopagima.school.students.domain.StudentAlreadyExistsException;
import com.jopagima.school.students.domain.StudentRepository;

/**
 * Entry-point adapter: translates the HTTP proxy event into a domain Student and
 * back into an HTTP response. Domain exceptions are the single source of truth for
 * what HTTP status is returned — this handler never encodes business rules itself.
 */
public class RegisterStudentHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegisterStudentHandler(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {


        if (input == null || input.getBody() == null || input.getBody().isBlank()) {
            return buildResponse(400, "Malformed request body");
        }

        try {
            RegisterStudentRequest request = objectMapper.readValue(input.getBody(), RegisterStudentRequest.class);
            Student student = request.toDomain();
            studentRepository.save(student);
            return buildResponse(201, null);
        } catch (JsonProcessingException e) {
            return buildResponse(400, "Malformed JSON body");
        } catch (InvalidStudentException e) {
            return buildResponse(400, e.getMessage());
        } catch (StudentAlreadyExistsException e) {
            return buildResponse(409, e.getMessage());
        }
    }

    private APIGatewayV2HTTPResponse buildResponse(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(body)
                .build();
    }
}
