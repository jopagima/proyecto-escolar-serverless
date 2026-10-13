package com.jopagima.school.infra.students;

import java.util.List;

import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolGroupOptions;
import software.constructs.Construct;

/**
 * StudentsUserPoolConstruct
 */
public class StudentsUserPoolConstruct extends Construct {

    private final UserPool userPool;

    public StudentsUserPoolConstruct(final Construct scope, String id) {
        super(scope, id);
        this.userPool = UserPool.Builder.create(this, "StudentsUserPool")
                .selfSignUpEnabled(false)
                .signInAliases(SignInAliases.builder()
                        .email(true).build())
                .accountRecovery(AccountRecovery.EMAIL_ONLY)
                .build();
        createUserPoolGroups(List.of("student", "teacher", "admin"));
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

}
