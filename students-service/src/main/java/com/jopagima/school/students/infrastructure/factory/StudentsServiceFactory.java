package com.jopagima.school.students.infrastructure.factory;

import com.jopagima.school.students.application.RegisterStudentUseCase;
import com.jopagima.school.students.domain.repositories.StudentRepository;
import com.jopagima.school.students.infrastructure.adapters.DynamoDbStudentRepository;

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

    private static StudentRepository getStudentRepository() {
        DynamoDbClient client = DynamoDbClient.create();
        String tableName = System.getenv("TABLE_NAME");
        return new DynamoDbStudentRepository(client, tableName);
    }
}
