package com.jopagima.school.courses.infrastructure;

import java.util.Map;

import com.jopagima.school.courses.domain.Course;
import com.jopagima.school.courses.domain.CourseAlreadyExistsException;
import com.jopagima.school.courses.domain.CourseRepository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import com.jopagima.school.commons.domain.Id;
import java.util.Optional;

/**
 * DynamoDbCourseRepository
 */
public class DynamoDbCourseRepository implements CourseRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbCourseRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

	@Override
	public void save(Course course) throws CourseAlreadyExistsException {
        // TODO 4: build the item Map<String, AttributeValue> with:
        //   PK = "COURSE#" + course.getId(), SK = "METADATA",
        //   name as a string attribute, maxCapacity as a NUMBER attribute
        //   (AttributeValue.builder().n(String.valueOf(course.getMaxCapacity())).build()
        //   — note: numbers in DynamoDB attribute values are always strings on the wire).

        
        Map<String, AttributeValue> item = Map.of("PK", AttributeValue.builder().s("COURSE#" + course.getId()).build(),
                "SK", AttributeValue.builder().s("METADATA").build(),
                "name", AttributeValue.builder().s(course.getName()).build(),
                "maxCapacity", AttributeValue.builder().n(String.valueOf(course.getMaxCapacity())).build());    

        // TODO 5: build a PutItemRequest with tableName, the item above, and
        //   conditionExpression("attribute_not_exists(PK)").
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .conditionExpression("attribute_not_exists(PK)")
                .build();   

        // TODO 6: call dynamoDbClient.putItem(request), catching
        //   ConditionalCheckFailedException and rethrowing as
        //   CourseAlreadyExistsException(course.getId()).
        try{
            dynamoDbClient.putItem(request);
        } catch (ConditionalCheckFailedException e) {
            throw new CourseAlreadyExistsException("Course " + course.getId() + " already exists");
        }         
	}

    @Override
    public Optional<Course> findById(Id id) {
        // TODO 7: implement findById using dynamoDbClient.getItem() and
        //   Optional.ofNullable() to return an Optional<Course>.

        Map<String, AttributeValue> key = Map.of("PK", AttributeValue.builder().s("COURSE#" + id).build(),
                "SK", AttributeValue.builder().s("METADATA").build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        GetItemResponse response = dynamoDbClient.getItem(request); 

        if (response.item() == null || response.item().isEmpty()) {
            return Optional.empty();
        }


        var item = response.item();
        Id idFromDataBase = Id.generateFromPlainTextIdentifier(item.get("id").s());
        String name = item.get("name").s();
        int maxCapacity = Integer.parseInt(item.get("maxCapacity").n());
        Course course =  Course.reconstitute(idFromDataBase, name, maxCapacity);
        return Optional.of(course);
            

    } 



}
