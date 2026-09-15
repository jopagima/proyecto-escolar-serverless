package com.jopagima.school.infra.courses;




import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.constructs.Construct;

/**
 * CDK construct provisioning the Cursos (Courses) bounded-context table.
 *
 * Design decision: Course items (PK=COURSE#<id>, SK=METADATA) and CourseEnrollment
 * items (PK=COURSE#<id>, SK=STUDENT#<studentId>) share this table via the adjacency
 * list pattern. The StudentCoursesIndex GSI (GSI1PK=STUDENT#<studentId>,
 * GSI1SK=COURSE#<courseId>) answers "which courses is this student enrolled in"
 * without scanning the table — only enrollment items populate GSI1PK/GSI1SK.
 */
public class CoursesTableConstruct extends Construct {

    private final Table table;
    private static final String TABLE_NAME = "CoursesTable";


	public CoursesTableConstruct(final Construct scope, final String id) {

		super(scope, id);

        this.table = Table.Builder.create(this, id)
            .tableName(TABLE_NAME)
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

               // TODO 1: add the StudentCoursesIndex GSI to `table` via
        table.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
            .indexName("StudentCoursesIndex")
            .partitionKey(Attribute.builder().name("GSI1PK").type(AttributeType.STRING).build())
            .sortKey(Attribute.builder().name("GSI1SK").type(AttributeType.STRING).build())
            .projectionType(ProjectionType.KEYS_ONLY)
            .build());

    }

    public Table getTable() {
		return table;
	}

    public String getTableName() {
        return TABLE_NAME;
    }   

}
