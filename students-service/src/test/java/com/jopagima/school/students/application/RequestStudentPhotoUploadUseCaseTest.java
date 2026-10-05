package com.jopagima.school.students.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.jopagima.school.students.application.ports.PhotoStoragePort;
import com.jopagima.school.commons.domain.Id;
import com.jopagima.school.commons.domain.ValidationError;


import org.junit.jupiter.api.Test;



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
        String uploadUrl = useCase.execute(studentId.toString());
        assertTrue(uploadUrl.contains("students/" + studentId + "/photo"));
   }  

  @Test
  void  doesNotAllowMalformedStudentId (){
        PhotoStoragePort photoStoragePort = new PhotoStoragePort() {
            @Override
            public String generateUploadUrl(String objectKey) {
                return "https://fake-bucket/upload/" + objectKey + "?signature=test";
            }
        };
        RequestStudentPhotoUploadUseCase useCase = new RequestStudentPhotoUploadUseCase(photoStoragePort);
        String studentId = "st-123456";
        assertThrows(ValidationError.class,
                () -> useCase.execute(studentId));
  }  
}
