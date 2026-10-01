package com.jopagima.school.students.infrastructure.adapters;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class S3PhotoStorageAdapterTest {

    @Test
    void generatesPresignedPutUrlForBucketAndKey() {
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.EU_WEST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .build();
        S3PhotoStorageAdapter adapter = new S3PhotoStorageAdapter(presigner, "students-photos-test");

        String url = adapter.generateUploadUrl("students/123/photo");

        assertTrue(url.contains("students-photos-test"));
        assertTrue(url.contains("students/123/photo"));
        assertTrue(url.contains("X-Amz-Signature="));
    }
}
