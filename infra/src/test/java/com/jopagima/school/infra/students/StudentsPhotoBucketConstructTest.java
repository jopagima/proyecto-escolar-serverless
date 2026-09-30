package com.jopagima.school.infra.students;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.assertions.Template;

import java.util.Map;

class StudentsPhotoBucketConstructTest {

    @Test
    void createsPrivateBucketWithoutPublicAccess() {
        Stack stack = new Stack();
        new StudentsPhotoBucketConstruct(stack, "StudentsPhotoBucket");

        Template template = Template.fromStack(stack);

        template.hasResourceProperties("AWS::S3::Bucket", Map.of(
                "PublicAccessBlockConfiguration", Map.of(
                        "BlockPublicAcls", true,
                        "BlockPublicPolicy", true,
                        "IgnorePublicAcls", true,
                        "RestrictPublicBuckets", true
                )
        ));
    }
}
