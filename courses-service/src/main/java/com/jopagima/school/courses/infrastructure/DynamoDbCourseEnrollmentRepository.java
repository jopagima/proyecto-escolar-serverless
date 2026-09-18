package com.jopagima.school.courses.infrastructure;

import java.util.Map;

import com.jopagima.school.courses.domain.CourseEnrollment;
import com.jopagima.school.courses.domain.CourseEnrollmentRepository;
import com.jopagima.school.courses.domain.EnrollmentAlreadyExistsException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * DynamoDbCourseEnrollmentRepository
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
            throw new EnrollmentAlreadyExistsException(enrollment.getCourseId(), enrollment.getStudentId());
        } 
		
	}

	@Override
	public int countEnrollments(String courseId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'countEnrollments'");
	}



}
