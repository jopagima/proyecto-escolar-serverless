package com.jopagima.school.infra;

import com.jopagima.school.infra.pipeline.PipelineStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class ServerlessSchoolManagementApp {
    public static void main(String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();
            
        new PipelineStack(app, "SchoolServerlessPipelineStack", StackProps.builder().env(env).build());
        app.synth();
    }

}
