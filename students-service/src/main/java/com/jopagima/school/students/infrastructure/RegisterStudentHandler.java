package com.jopagima.school.students.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import com.jopagima.school.commons.domain.DomainError;
import com.jopagima.school.commons.domain.ErrorType;
import com.jopagima.school.commons.domain.ValidationError;
import com.jopagima.school.students.application.RegisterStudentUseCase;
import com.jopagima.school.students.infrastructure.factory.StudentsServiceFactory;
import com.jopagima.school.students.infrastructure.observability.AccessMetrics;
import com.jopagima.school.students.infrastructure.observability.DenialReason;
import com.jopagima.school.students.infrastructure.security.CallerGroups;

/**
 * Entry-point adapter: translates the HTTP proxy event into a call to
 * RegisterStudentUseCase and back into an HTTP response. ValidationError and DomainError.getType() are the single
 * source of truth for what HTTP status is returned — this handler never encodes business rules itself.
 */
public class RegisterStudentHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final RegisterStudentUseCase registerStudentUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String OPERATION = "RegisterStudent";
    private final AccessMetrics metrics;


    public RegisterStudentHandler() {
        this(StudentsServiceFactory.createRegisterStudentUseCase(), StudentsServiceFactory.createAccessMetrics());
    }
    public RegisterStudentHandler(RegisterStudentUseCase registerStudentUseCase) {
        this(registerStudentUseCase, AccessMetrics.noop());
    }

    public RegisterStudentHandler(RegisterStudentUseCase useCase, AccessMetrics metrics) {
        this.registerStudentUseCase = useCase;
        this.metrics = metrics;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        CallerGroups groupsFromJwt = CallerGroups.from(input);
        if (!groupsFromJwt.hasAnyOf("teacher", "admin") ) {
            DenialReason denialReason = (groupsFromJwt.isEmpty()) ? DenialReason.NO_GROUPS : DenialReason.GROUP_NOT_ALLOWED;
            metrics.recordDenied(OPERATION, denialReason);
            return buildResponse(403,  "Only teachers and admins are allowed to register students");
        }
        if (input.getBody() == null || input.getBody().isBlank()) {
            return buildResponse(400, "Malformed request body");
        }
        try {
            RegisterStudentRequest request = objectMapper.readValue(input.getBody(), RegisterStudentRequest.class);
            String studentId = registerStudentUseCase.execute(
                    request.getFirstName(), request.getLastName(), request.getEmail());
            return buildResponse(201, objectMapper.writeValueAsString(Map.of("id", studentId)));
        } catch (JsonProcessingException e) {
            return buildResponse(400, "Malformed JSON body");
        } catch (ValidationError e) {
            return buildResponse(422, e.getMessage());
        } catch (DomainError e) {
            return buildResponse(statusFor(e.getType()), e.getMessage());
        }
    }

    private static int statusFor(ErrorType type) {
        return switch (type) {
            case notFound -> 404;
            case alreadyExists -> 409;
            case other -> 400;
        };
    }

    private APIGatewayV2HTTPResponse buildResponse(int statusCode, String body) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withBody(body)
                .build();
    }
}
