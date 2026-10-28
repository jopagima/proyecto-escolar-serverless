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
import com.jopagima.school.students.infrastructure.observability.AccessMetrics;
import com.jopagima.school.students.infrastructure.observability.DenialReason;
import com.jopagima.school.students.infrastructure.security.CallerGroups;

public class RequestStudentPhotoUploadHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final RequestStudentPhotoUploadUseCase useCase;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AccessMetrics metrics;

    public RequestStudentPhotoUploadHandler() {
        this(StudentsServiceFactory.createRequestStudentPhotoUploadUseCase(), AccessMetrics.noop());
    }

    public RequestStudentPhotoUploadHandler(RequestStudentPhotoUploadUseCase useCase) {
        this(useCase, AccessMetrics.noop());
    }

    public RequestStudentPhotoUploadHandler(RequestStudentPhotoUploadUseCase useCase, AccessMetrics metrics) {
        this.useCase = useCase;
        this.metrics = metrics;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        CallerGroups groupsFromJwt = CallerGroups.from(input);
        if (!groupsFromJwt.hasAnyOf("teacher", "admin") ) {
            metrics.recordDenied("RequestPhotoUpload", DenialReason.GROUP_NOT_ALLOWED);
            return response(403, Map.of("error", "Only teachers and admins are allowed to request photo upload URLs"));
        }
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


