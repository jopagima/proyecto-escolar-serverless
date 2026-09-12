package com.jopagima.school.infra.pipeline;



import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.CodeBuildStep;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.constructs.Construct;

import java.util.List;

/**
 * Self-mutating CDK Pipeline: synthesizes the app, updates itself if its own
 * structure changed, then deploys SchoolServerlessStage. No manual `cdk deploy` is
 * needed once this pipeline exists — every push to the tracked branch triggers it.
 */
public class PipelineStack extends Stack {

    public PipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // TODO 1: replace with your real CodeStar Connection ARN (created manually
        //   in the console beforehand) and your GitHub "owner/repo" string.
        CodePipelineSource source = CodePipelineSource.connection(
                    "jopagima/proyecto-escolar-serverless",
                    "main",
                    software.amazon.awscdk.pipelines.
                        ConnectionSourceOptions.builder()
                        .connectionArn("arn:aws:codeconnections:eu-west-1:477740190077:connection/e26991a7-ebd7-4572-8097-b93748eb1e36")
                        .build()
                );
                // TODO 2: fill in the real build commands. You need at least:
                //   - "mvn clean package" (compiles + runs all module tests — this
                //     IS your test gate, no separate test stage needed)
                //   - "npx cdk synth" (produces the CloudFormation templates the
                //     pipeline itself deploys)                
        CodeBuildStep synthStep = CodeBuildStep.Builder.create("Synth")
                .input(source)
                .commands(List.of(
                    "npm install -g aws-cdk@2.1128.1", // install CDK CLI
                    "mvn clean package",       // build the Java Lambda artifact
                    "cd infra && cdk synth"                // synthesize the CDK app
                ))
                .primaryOutputDirectory("infra/cdk.out")
                .build();

        CodePipeline pipeline = CodePipeline.Builder.create(this, "SchoolPipeline")
            .pipelineName("SchoolPipeline")
            .synth(synthStep)
            .build();

        // TODO 3: add the deployment stage:
        pipeline.addStage(new SchoolServerlessStage(this, "Deploy",
               StageProps.builder().env(props.getEnv()).build())); 
               
   
    }

}