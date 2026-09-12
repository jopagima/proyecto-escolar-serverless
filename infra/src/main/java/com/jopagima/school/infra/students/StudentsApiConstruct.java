package com.jopagima.school.infra.students;


import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpLambdaIntegration;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.constructs.Construct;
import java.io.File;
import java.util.Map;

public class StudentsApiConstruct extends Construct {

    private static final String JAR_RELATIVE_TO_REPO_ROOT =
            "students-service/target/students-service-1.0-SNAPSHOT.jar";
    private static final String JAR_RELATIVE_TO_INFRA_MODULE =
            "../students-service/target/students-service-1.0-SNAPSHOT.jar";


    private final Function registerStudentFunction;

    public Function getRegisterStudentFunction() {
        return registerStudentFunction;
    }

    public StudentsApiConstruct(Construct scope, String id, Table table) {
        super(scope, id);


        // Resolve the jar path relative to the repository root (the parent of the
        // "infra" module directory), regardless of which working directory the
        // CDK CLI / Maven process was launched from.


        this.registerStudentFunction = Function.Builder.create(this, "RegisterStudentFunction")
            .runtime(Runtime.JAVA_17)
            .functionName("RegisterStudentFunction")
             .code(Code.fromAsset(resolveStudentsServiceJarPath()))
            .handler("com.jopagima.school.students.infrastructure.RegisterStudentHandler::handleRequest")
                .memorySize(512)
                .timeout(Duration.seconds(15))
                .tracing(Tracing.ACTIVE) // Habilita Observabilidad con X-Ray [6]            
            .environment(Map.of(
                "TABLE_NAME", table.getTableName()
            ))
            .build();
        table.grantWriteData(this.registerStudentFunction);
        // TODO 6: grant registerStudentFunction write access to studentsTable using
        //   studentsTable.grantWriteData(registerStudentFunction) — minimum privilege,
        //   this Lambda only writes today.

        HttpApi httpApi = HttpApi.Builder.create(this, "StudentsHttpApi").build();

        httpApi.addRoutes(AddRoutesOptions.builder()
           .path("/students")
           .methods(java.util.List.of(HttpMethod.POST))
           .integration(new HttpLambdaIntegration("RegisterStudentIntegration", registerStudentFunction))
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
