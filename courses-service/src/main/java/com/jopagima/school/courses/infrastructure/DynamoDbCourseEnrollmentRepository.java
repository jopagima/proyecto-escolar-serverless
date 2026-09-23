package com.jopagima.school.courses.infrastructure;

import java.util.Map;

import com.jopagima.school.courses.domain.CourseEnrollment;
import com.jopagima.school.courses.domain.CourseEnrollmentRepository;
import com.jopagima.school.courses.domain.EnrollmentAlreadyExistsException;
import com.jopagima.school.commons.domain.Id;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * Infrastructure adapter for CourseEnrollmentRepository. countEnrollments() relies on
 * the adjacency list design from Fase 2 Día 1: all enrollment items for a course share
 * the same PK, with SK prefixed "STUDENT#", so a single Query with begins_with and
 * Select.COUNT answers the count without fetching item attributes.
 */
public class DynamoDbCourseEnrollmentRepository implements CourseEnrollmentRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

	public DynamoDbCourseEnrollmentRepository(DynamoDbClient dynamoDbClient, String tableName) {
		this.dynamoDbClient = dynamoDbClient;
		this.tableName = tableName;
    }

    @Override
	public void enroll(CourseEnrollment enrollment) throws EnrollmentAlreadyExistsException {
		// TODO 4: build the item Map<String, AttributeValue> with:
        //   PK = "COURSE#" + enrollment.getCourseId(), SK = "STUDENT#" + enrollment.getStudentId().

		Map<String, AttributeValue> item = Map.of("PK", AttributeValue.builder().s("COURSE#" + enrollment.getCourseId()).build(),
                "SK", AttributeValue.builder().s("STUDENT#" + enrollment.getStudentId()).build());    

        // TODO 5: build a PutItemRequest with tableName, the item above, and
        //   conditionExpression("attribute_not_exists(PK)").
		PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .conditionExpression("attribute_not_exists(PK)")
                .build(); 

        // TODO 6: call dynamoDbClient.putItem(request), catching
        //   ConditionalCheckFailedException and rethrowing as
        //   EnrollmentAlreadyExistsException(enrollment.getCourseId(), enrollment.getStudentId()).

		try{
            dynamoDbClient.putItem(request);
        } catch (ConditionalCheckFailedException e) {
            throw new EnrollmentAlreadyExistsException(""+enrollment.getCourseId(), enrollment.getStudentId());
        } 
		
	}

	@Override
	public int countEnrollments(Id courseId) {
        // TODO 7: build a QueryRequest with:
        //   tableName, select(Select.COUNT),
        //   keyConditionExpression("PK = :pk AND begins_with(SK, :skPrefix)"),
        //   expressionAttributeValues mapping ":pk" -> "COURSE#" + courseId,
        //   ":skPrefix" -> "STUDENT#".

		QueryRequest request = QueryRequest.builder()
				.tableName(tableName)
				.select(software.amazon.awssdk.services.dynamodb.model.Select.COUNT)
				.keyConditionExpression("PK = :pk AND begins_with(SK, :skPrefix)")
				.expressionAttributeValues(Map.of(
						":pk", AttributeValue.builder().s("COURSE#" + courseId).build(),
						":skPrefix", AttributeValue.builder().s("STUDENT#").build()
				))
				.build();

        // TODO 8: call dynamoDbClient.query(request) and return response.count().
		QueryResponse response = dynamoDbClient.query(request);
		return response.count();
	}



}
