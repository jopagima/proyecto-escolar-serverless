package com.jopagima.school.infra.students;



import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.constructs.Construct;


/**
 * CDK construct provisioning the Alumnos (Students) bounded-context table.
 *
 * Design decision: PK/SK overloading ("STUDENT#<id>" / "METADATA") is a project-wide
 * convention applied consistently across all five bounded-context tables, to keep the
 * key design extensible without a future migration, even where a single item type
 * exists today.
 */
public class StudentsTableConstruct extends Construct {
    private final Table table;

    public StudentsTableConstruct(final Construct scope, final String id) {
        super(scope, id);

        table = Table.Builder.create(this, "StudentsTable")
            .tableName("StudentsTable")
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .partitionKey(Attribute.builder()
                .name("PK")
                .type(AttributeType.STRING)
                
                .build())
            .sortKey(Attribute.builder()
                .name("SK")
                .type(AttributeType.STRING)
                .build())
            .build();
    }




    public Table getTable() {
        return table;
    }

}
