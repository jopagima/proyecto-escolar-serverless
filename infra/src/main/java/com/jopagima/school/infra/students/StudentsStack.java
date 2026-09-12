package com.jopagima.school.infra.students;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

/**
 * Composition root for the Students bounded context: wires the DynamoDB table
 * (Day 1) to the Lambda + HTTP API (Day 3). This is the unit the pipeline deploys.
 */
public class StudentsStack extends Stack {

    public StudentsStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Create the DynamoDB table
        StudentsTableConstruct studentsTable = new StudentsTableConstruct(this, "StudentsTable");

        // Create the Lambda function and API Gateway
        new StudentsApiConstruct (this, "StudentsApi", studentsTable.getTable());
    }

}
