package com.jopagima.school.students.infrastructure.adapters;


import com.jopagima.school.students.application.ports.PhotoStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

public class S3PhotoStorageAdapter implements PhotoStoragePort {

    Logger logger = LoggerFactory.getLogger(S3PhotoStorageAdapter.class);

    private final S3Presigner presigner;
    private final String bucketName;

    public S3PhotoStorageAdapter(S3Presigner presigner, String bucketName) {
        this.presigner = presigner;
        this.bucketName = bucketName;
    }

    @Override
    public String generateUploadUrl(String objectKey) {
 
        PutObjectRequest  putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
                
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        logger.info("Generated pre-signed URL for S3 upload: {}", presignedRequest.url().toString());
        return presignedRequest.url().toString();
    }

}
