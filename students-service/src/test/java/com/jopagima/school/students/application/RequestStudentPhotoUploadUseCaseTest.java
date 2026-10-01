package com.jopagima.school.students.application;


import com.jopagima.school.students.application.ports.PhotoStoragePort;
import com.jopagima.school.commons.domain.Id;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class RequestStudentPhotoUploadUseCaseTest {
   @Test
   void generatesUploadUrlKeyedByStudentId(){
        PhotoStoragePort photoStoragePort = new PhotoStoragePort() {
            @Override
            public String generateUploadUrl(String objectKey) {
                return "https://fake-bucket/upload/" + objectKey + "?signature=test";
            }
        };
        RequestStudentPhotoUploadUseCase useCase = new RequestStudentPhotoUploadUseCase(photoStoragePort);
        Id studentId = Id.generateUniqueIdentifier();
        String uploadUrl = useCase.execute(studentId);
        assertTrue(uploadUrl.contains("students/" + studentId + "/photo"));
   }  
}
