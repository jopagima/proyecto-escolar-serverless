package com.jopagima.school.students.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

public class CallerGroupsTest {
    @Test 
    public void readSeveralGroupsFromTheClaim() {
        // Arrange
        CallerGroups callerGroups = CallerGroups.parse("[teacher admin]");


        // Act
        

        // Assert
        assertTrue(callerGroups.hasAnyOf("admin"));
        assertTrue(callerGroups.hasAnyOf("teacher"));
    }
    
    @Test
    void readsASingleGroup() {
        CallerGroups callerGroups = CallerGroups.parse("[student]");
        assertTrue(callerGroups.hasAnyOf("student"));
    }
    
    @Test
    void deniesWhenTheClaimHasNoBrackets() {
        assertFalse(CallerGroups.parse("xadminx").hasAnyOf("admin"));
    }

    @Test
    void createsNoGroupsFromEmptyBrackets() {
        assertFalse(CallerGroups.parse("[]").hasAnyOf(""));
    }    

    @Test
    void deniesWhenTheClaimIsMissing() {
        assertFalse(CallerGroups.parse(null).hasAnyOf("teacher", "admin"));
    }

    @Test
    void toleratesExtraSpacesAndRepeatedGroups() {
        assertTrue(CallerGroups.parse("[teacher  admin  teacher  admin]").hasAnyOf("teacher"));
    } 

    @Test
    void doesNotMatchPartialGroupNames() {
        assertFalse(CallerGroups.parse("[superadmin]").hasAnyOf("admin"));
    } 
    
    @Test
    void readsGroupsFromTheJwtClaimsOfTheEvent() {
        APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt = new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT();
        jwt.setClaims(Map.of("cognito:groups", "[teacher]"));
        APIGatewayV2HTTPEvent.RequestContext.Authorizer authorizer = new APIGatewayV2HTTPEvent.RequestContext.Authorizer();
        authorizer.setJwt(jwt);
        APIGatewayV2HTTPEvent.RequestContext context = new APIGatewayV2HTTPEvent.RequestContext();
        context.setAuthorizer(authorizer);
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRequestContext(context);

        assertTrue(CallerGroups.from(event).hasAnyOf("teacher"));
    }      
}
