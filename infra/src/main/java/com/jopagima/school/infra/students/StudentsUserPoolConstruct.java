package com.jopagima.school.infra.students;

import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.aws_apigatewayv2_authorizers.HttpUserPoolAuthorizer;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolGroupOptions;
import software.amazon.awscdk.services.cognito.UserPoolClientOptions;
import software.constructs.Construct;

/**
 * StudentsUserPoolConstruct
 */
public class StudentsUserPoolConstruct extends Construct {

    private final UserPool userPool;
    private final UserPoolClient userPoolClient;
    private final HttpUserPoolAuthorizer authorizer;


    public StudentsUserPoolConstruct(final Construct scope, String id) {
        super(scope, id);
        this.userPool = UserPool.Builder.create(this, "StudentsUserPool")
                .selfSignUpEnabled(false)
                .signInAliases(SignInAliases.builder()
                        .email(true).build())
                .accountRecovery(AccountRecovery.EMAIL_ONLY)
                .build();
        createUserPoolGroups(List.of("student", "teacher", "admin"));

        UserPoolClientOptions userPoolClientOptions = UserPoolClientOptions.builder()
            .generateSecret(false)
            .authFlows(AuthFlow.builder()
                .userPassword(true)
                .build())
            .build();

        this.userPoolClient = this.userPool.addClient("StudentApiClient", 
            userPoolClientOptions);

        this.authorizer =  HttpUserPoolAuthorizer.Builder.create( "StudentsAuthorizer", this.userPool)
            .userPoolClients(List.of(this.userPoolClient))
            .build();

            // TODO 5: two CfnOutput, UserPoolId and UserPoolClientId, so you can read them in
        //   CloudFormation when creating test users.
        CfnOutput.Builder.create(this, "UserPoolId")
            .value(this.userPool.getUserPoolId())
            .build();    
        CfnOutput.Builder.create(this, "UserPoolClientId")
            .value(this.userPoolClient.getUserPoolClientId())
            .build();
    }

    private void createUserPoolGroups(List<String> userGroups) {
        for (String groupName : userGroups) {
            this.userPool.addGroup(groupName, 
                UserPoolGroupOptions.builder()
                    .groupName(groupName)
                    .build());
        }
    }

    public UserPool getUserPool() {
        return userPool;
    }
    public UserPoolClient getUserPoolClient() {
        return userPoolClient;
    }
    public HttpUserPoolAuthorizer getAuthorizer() {
        return authorizer;
    }

}
