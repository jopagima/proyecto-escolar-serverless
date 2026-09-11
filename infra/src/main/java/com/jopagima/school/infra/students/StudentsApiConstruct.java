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
import java.util.Map;

public class StudentsApiConstruct extends Construct {


    private final Function registerStudentFunction;

    public Function getRegisterStudentFunction() {
        return registerStudentFunction;
    }

    public StudentsApiConstruct(Construct scope, String id, Table table) {
        super(scope, id);
        this.registerStudentFunction = Function.Builder.create(this, "RegisterStudentFunction")
            .runtime(Runtime.JAVA_17)
            .functionName("RegisterStudentFunction")
             .code(Code.fromAsset("../students-service/target/students-service-1.0-SNAPSHOT.jar"))
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

}
