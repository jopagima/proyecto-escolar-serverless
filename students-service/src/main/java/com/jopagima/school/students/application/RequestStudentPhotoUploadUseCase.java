package com.jopagima.school.students.application;


import com.jopagima.school.students.application.ports.PhotoStoragePort;
import com.jopagima.school.commons.domain.Id;

public class RequestStudentPhotoUploadUseCase {
    private final  PhotoStoragePort photoStoragePort;

    public RequestStudentPhotoUploadUseCase(PhotoStoragePort photoStoragePort) {
        this.photoStoragePort = photoStoragePort;
    }

    public String execute(String studentId) {
        Id id = Id.generateFromPlainTextIdentifier(studentId);
        String objectKey = "students/" + id + "/photo";
        return photoStoragePort.generateUploadUrl(objectKey);

    }
}
