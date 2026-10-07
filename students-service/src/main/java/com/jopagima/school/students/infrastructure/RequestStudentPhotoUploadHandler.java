package com.jopagima.school.students.infrastructure;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jopagima.school.commons.domain.ValidationError;
import com.jopagima.school.students.application.RequestStudentPhotoUploadUseCase;
import com.jopagima.school.students.infrastructure.factory.StudentsServiceFactory;

public class RequestStudentPhotoUploadHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final RequestStudentPhotoUploadUseCase useCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequestStudentPhotoUploadHandler() {
        this.useCase = StudentsServiceFactory.createRequestStudentPhotoUploadUseCase();
        
    }

    public RequestStudentPhotoUploadHandler(RequestStudentPhotoUploadUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        String studentIdFromPath = input.getPathParameters().get("studentId");
        if(studentIdFromPath == null || studentIdFromPath.isEmpty()) {
            return response(400, Map.of("error", "Missing student ID in path parameters"));
        }
        try {
            String uploadUrl = useCase.execute(studentIdFromPath);
            return response(200, Map.of("uploadUrl", uploadUrl));
        } catch (ValidationError e) {
            return response(422, Map.of("error", e.getMessage()));
        }
    }

    

    private APIGatewayV2HTTPResponse response(int statusCode, Map<String, String> body) {
        try {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(statusCode)
                    .withBody(objectMapper.writeValueAsString(body))
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize response body", e);
        }
    }
}


