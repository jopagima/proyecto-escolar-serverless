package com.jopagima.school.students.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
}
