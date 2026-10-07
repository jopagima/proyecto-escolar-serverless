package com.jopagima.school.students.infrastructure;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.commons.domain.ValidationError;
import com.jopagima.school.students.application.RequestStudentPhotoUploadUseCase;
import com.jopagima.school.students.infrastructure.factory.StudentsServiceFactory;

public class RequestStudentPhotoUploadHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private final RequestStudentPhotoUploadUseCase useCase;

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
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(400)
                    .withBody("{\"error\": \"Missing student ID in path parameters\"}")
                    .build();
        }

            Id studentId = Id.generateFromPlainTextIdentifier(studentIdFromPath);
            String uploadUrl = useCase.execute(studentId.toString());
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withBody("{\"uploadUrl\": \"" + uploadUrl + "\"}")
                    .build();

    }
}


