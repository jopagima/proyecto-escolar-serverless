package com.jopagima.school.students.infrastructure;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.students.application.RequestStudentPhotoUploadUseCase;
import com.jopagima.school.students.application.ports.PhotoStoragePort;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RequestStudentPhotoUploadHandlerTest {
   @Test
   public void returnsUploadUrlForGivenStudentId(){
    PhotoStoragePort photoStoragePort = new PhotoStoragePort() {
        @Override
        public String generateUploadUrl(String objectKey) {
            return "https://fake-bucket/" + objectKey + "?signature=test";
        }
    };
    RequestStudentPhotoUploadUseCase useCase = new RequestStudentPhotoUploadUseCase(photoStoragePort);
    RequestStudentPhotoUploadHandler handler = new RequestStudentPhotoUploadHandler(useCase);

    Id studentId = Id.generateUniqueIdentifier();
    APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
    event.setPathParameters(Map.of("studentId", studentId.toString()));
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().contains("fake-bucket/students/" + studentId + "/photo"));
   }  

    @Test
    void returns400WhenPathParameterMissing() {
        PhotoStoragePort fakePort = objectKey -> "unused";
        RequestStudentPhotoUploadHandler handler =
                new RequestStudentPhotoUploadHandler(new RequestStudentPhotoUploadUseCase(fakePort));


    
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setPathParameters(Map.of());
        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(400, response.getStatusCode());
    }   

    @Test
    void returns422WhenStudentIdIsMalformed() {
        PhotoStoragePort fakePort = objectKey -> "unused";
        RequestStudentPhotoUploadHandler handler =
                new RequestStudentPhotoUploadHandler(new RequestStudentPhotoUploadUseCase(fakePort));


    
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setPathParameters(Map.of("studentId", "not-a-uuid"));
        APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);

        assertEquals(422, response.getStatusCode());
    }      
}
