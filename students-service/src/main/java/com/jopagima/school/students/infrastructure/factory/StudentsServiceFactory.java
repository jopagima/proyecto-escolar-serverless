package com.jopagima.school.students.infrastructure.factory;

import com.jopagima.school.students.application.RegisterStudentUseCase;
import com.jopagima.school.students.domain.repositories.StudentRepository;
import com.jopagima.school.students.infrastructure.adapters.DynamoDbStudentRepository;
import com.jopagima.school.students.application.RequestStudentPhotoUploadUseCase;
import com.jopagima.school.students.application.ports.PhotoStoragePort;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import com.jopagima.school.students.infrastructure.adapters.S3PhotoStorageAdapter;


import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Explicit wiring for students-service (guidelinesHexagonal-serverless.md §5): no DI
 * container, the Lambda handler's no-arg constructor is the composition root.
 */
public final class StudentsServiceFactory {

    private StudentsServiceFactory() {
    }

    public static RegisterStudentUseCase createRegisterStudentUseCase() {
        return new RegisterStudentUseCase(getStudentRepository());
    }

    public static RequestStudentPhotoUploadUseCase createRequestStudentPhotoUploadUseCase() {
        S3Presigner presigner = S3Presigner.create();
        String bucketName = System.getenv("BUCKET_NAME");   
        PhotoStoragePort photoStoragePort = new S3PhotoStorageAdapter(presigner, bucketName);
        return new RequestStudentPhotoUploadUseCase(photoStoragePort);
    }

    private static StudentRepository getStudentRepository() {
        DynamoDbClient client = DynamoDbClient.create();
        String tableName = System.getenv("TABLE_NAME");
        return new DynamoDbStudentRepository(client, tableName);
    }
}
