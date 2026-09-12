package com.jopagima.school.infra.pipeline;

import com.jopagima.school.infra.students.StudentsStack;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

/**
 * Groups every bounded-context stack that must be deployed together as one pipeline
 * run. Today it contains only StudentsStack; CoursesStack, ExamsStack, etc. join here
 * as their phases open — the pipeline does not need to change to pick them up.
 */
public class SchoolServerlessStage extends Stage {

    public SchoolServerlessStage(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SchoolServerlessStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);

        // Add the StudentsStack to this stage
        new StudentsStack(this, "StudentsStack", null);
    }

}
