package com.jopagima.school.infra.students;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.constructs.Construct;

public class StudentsPhotoBucketConstruct extends Construct {

    private final Bucket bucket;

    public StudentsPhotoBucketConstruct(Stack stack, String id) {
        super(stack, id);

        BucketProps props = BucketProps.builder()
                .blockPublicAccess(BlockPublicAccess.Builder.create()
                        .blockPublicAcls(true)
                        .blockPublicPolicy(true)
                        .ignorePublicAcls(true)
                        .restrictPublicBuckets(true)
                        .build())
                .build();

        this.bucket = new Bucket(this, "StudentsPhotoBucket", props);
    }

    public Bucket getBucket() {
        return bucket;
    }

}
