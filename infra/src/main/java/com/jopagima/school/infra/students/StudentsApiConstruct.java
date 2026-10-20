package com.jopagima.school.infra.students;


import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.IHttpRouteAuthorizer;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpLambdaIntegration;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;
import java.io.File;
import java.util.List;
import java.util.Map;

public class StudentsApiConstruct extends Construct {

    private static final String JAR_RELATIVE_TO_REPO_ROOT =
            "students-service/target/students-service-1.0-SNAPSHOT.jar";
    private static final String JAR_RELATIVE_TO_INFRA_MODULE =
            "../students-service/target/students-service-1.0-SNAPSHOT.jar";


    private final Function registerStudentFunction;
    private final Function photoUploadFunction;

    public Function getRegisterStudentFunction() {
        return registerStudentFunction;
    }

    public StudentsApiConstruct(Construct scope, String id, Table table, Bucket bucket, IHttpRouteAuthorizer authorizer) {
        super(scope, id);

        this.registerStudentFunction = createFunction(
                "RegisterStudentFunction",
                "RegisterStudentHandler",
                Map.of("TABLE_NAME", table.getTableName()));
        table.grantWriteData(this.registerStudentFunction);

        this.photoUploadFunction = createFunction(
                "PhotoUploadFunction",
                "RequestStudentPhotoUploadHandler",
                Map.of("PHOTO_BUCKET_NAME", bucket.getBucketName()));

        String studentsPhotosPrefix = "students/*";
        this.photoUploadFunction.addToRolePolicy(PolicyStatement.Builder.create()
            .effect(Effect.ALLOW)
            .actions(List.of("s3:PutObject"))
            .resources(List.of(bucket.arnForObjects(studentsPhotosPrefix)))
            .build());

        HttpApi httpApi = HttpApi.Builder.create(this, "StudentsHttpApi").build();
        addRoute(httpApi, "/students", HttpMethod.POST,
                "RegisterStudentIntegration", registerStudentFunction, authorizer);
        addRoute(httpApi, "/students/{studentId}/photo-upload-url", HttpMethod.GET,
                "PhotoUploadIntegration", photoUploadFunction, authorizer);
    }

    /**
     * Every students-service Lambda shares runtime, sizing, tracing and the same jar;
     * only its name, handler class and environment differ. The construct id is reused
     * as the physical function name, so both stay aligned.
     */
    private Function createFunction(String name, String handlerClassName, Map<String, String> environment) {
        String handlerPackage = "com.jopagima.school.students.infrastructure";
        String handlerMethod = "handleRequest";
        int memorySizeMb = 512;
        int timeoutSeconds = 15;

        return Function.Builder.create(this, name)
            .runtime(Runtime.JAVA_17)
            .functionName(name)
            .code(Code.fromAsset(resolveStudentsServiceJarPath()))
            .handler(handlerPackage + "." + handlerClassName + "::" + handlerMethod)
            .memorySize(memorySizeMb)
            .timeout(Duration.seconds(timeoutSeconds))
            .tracing(Tracing.ACTIVE)
            .environment(environment)
            .build();
    }

    private static void addRoute(HttpApi httpApi, String path, HttpMethod method,
                                 String integrationId, Function function, IHttpRouteAuthorizer autorizer) {
        httpApi.addRoutes(AddRoutesOptions.builder()
            .path(path)
            .methods(List.of(method))
            .integration(new HttpLambdaIntegration(integrationId, function))
            .authorizer(autorizer)
            .build());
    }

    /**
     * Design decision: the students-service jar is located relative to the working
     * directory of the process, which differs depending on the caller — `cdk synth`
     * (invoked from the repo root, where cdk.json lives) vs. Maven Surefire running
     * this construct's tests (invoked with the infra module as working directory).
     * Both candidates are tried explicitly rather than assuming one caller.
     */
    private static String resolveStudentsServiceJarPath() {
        if (new File(JAR_RELATIVE_TO_REPO_ROOT).exists()) {
            return JAR_RELATIVE_TO_REPO_ROOT;
        }
        if (new File(JAR_RELATIVE_TO_INFRA_MODULE).exists()) {
            return JAR_RELATIVE_TO_INFRA_MODULE;
        }
        throw new IllegalStateException(
                "Cannot locate students-service jar from working directory "
                        + new File("").getAbsolutePath()
                        + ". Run `mvn clean package` first.");
    }

}
