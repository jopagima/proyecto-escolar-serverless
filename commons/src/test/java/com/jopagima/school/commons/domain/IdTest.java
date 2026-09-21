package com.jopagima.school.commons.domain;


import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IdTest {
    private static final String UUID_PATTERN = "[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}";

    @Test
    void generatesValidIdentifier() {
        Id id = Id.generateUniqueIdentifier();
        assertTrue(Pattern.compile(UUID_PATTERN).matcher(id.toString()).matches());
    }   
    
    @Test
    void createsIdFromValidIdentifier() {
        UUID uuid = UUID.randomUUID();
        Id id = Id.generateFromPlainTextIdentifier(String.valueOf(uuid));
        assertEquals(String.valueOf(uuid), id.toString());
    }

    @Test
    public void failsFromInvalidIdentifier() {

        Exception exception = assertThrows(ValidationError.class, () -> {
            Id id = Id.generateFromPlainTextIdentifier("badUuidIdentifier");
        });
        String expectedMessage = "Invalid UUID format.";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }  
    
    @Test
    void treatsTwoIdsAsEqualWithSamePlainText() {
        UUID uuid = UUID.randomUUID();

        Id first = Id.generateFromPlainTextIdentifier(String.valueOf(uuid));
        Id second = Id.generateFromPlainTextIdentifier(String.valueOf(uuid));

        assertEquals(first, second);
    }
      
    @Test
    void treatsTwoGeneratedIdsAsDifferent() {
        Id first = Id.generateUniqueIdentifier();
        Id second = Id.generateUniqueIdentifier();

        assertNotEquals(first, second);
    }    

}
