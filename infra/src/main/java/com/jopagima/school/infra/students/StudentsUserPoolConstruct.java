package com.jopagima.school.infra.students;

import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.UserPool;
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
    }

    public UserPool getUserPool() {
        return userPool;
    }

}
