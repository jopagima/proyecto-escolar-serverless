package com.jopagima.school.infra.pipeline;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Template;

import java.util.Map;
public class PipelineStackTest {

    @Test
    public void testPipelineStack() {
        App app = new App();
        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();
        StackProps stackProps = StackProps.builder().env(env).build();
        PipelineStack pipelineStack = new PipelineStack(app, "TestPipelineStack", stackProps);

        // Generate the CloudFormation template from the stack
        Template template = Template.fromStack(pipelineStack);

        // Assert that the pipeline has been created with the expected properties
        template.hasResourceProperties("AWS::CodePipeline::Pipeline", Map.of(
                "Name", "SchoolPipeline"
        ));
    }

}
