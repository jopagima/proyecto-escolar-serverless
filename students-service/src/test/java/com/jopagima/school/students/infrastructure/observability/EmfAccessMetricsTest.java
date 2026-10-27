package com.jopagima.school.students.infrastructure.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



public class EmfAccessMetricsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> lines = new ArrayList<>();
    private final EmfAccessMetrics metrics = new EmfAccessMetrics(lines::add);


    @Test 
    void writesTheMetricDefinitionCloudWatchNeedsToExtractIt() throws Exception {
        metrics.recordDenied("RegisterStudent", DenialReason.GROUP_NOT_ALLOWED);

        JsonNode line = objectMapper.readTree(lines.get(0));
        JsonNode definition = line.at("/_aws/CloudWatchMetrics/0");
        assertEquals("SchoolPlatform/Access", definition.at("/Namespace").asText());
        assertEquals("AccessDenied", definition.at("/Metrics/0/Name").asText());
        assertEquals("Count", definition.at("/Metrics/0/Unit").asText());
        assertEquals("Operation", definition.at("/Dimensions/0/0").asText());
        assertEquals("Reason", definition.at("/Dimensions/0/1").asText());
        assertTrue(line.at("/_aws/Timestamp").isNumber());
    }

    @Test
    void writesTheValueAndTheDimensionValuesAtTheTopLevel() throws Exception {
        metrics.recordDenied("RegisterStudent", DenialReason.NO_GROUPS);

        JsonNode line = objectMapper.readTree(lines.get(0));
        assertEquals(1, line.get("AccessDenied").asInt());
        assertEquals("RegisterStudent", line.get("Operation").asText());
        assertEquals("NoGroups", line.get("Reason").asText());
    }  
    
    @Test
    void publishesBothReasonsWithTheirStableLabels() throws Exception {
        metrics.recordDenied("RegisterStudent", DenialReason.GROUP_NOT_ALLOWED);
        metrics.recordDenied("RegisterStudent", DenialReason.NO_GROUPS);

        assertEquals("GroupNotAllowed", objectMapper.readTree(lines.get(0)).get("Reason").asText());
        assertEquals("NoGroups", objectMapper.readTree(lines.get(1)).get("Reason").asText());
    } 
    
    @Test
    void emitsExactlyOneSingleLineDocumentPerDenial() {
        metrics.recordDenied("RegisterStudent", DenialReason.GROUP_NOT_ALLOWED);

        assertEquals(1, lines.size());
        assertFalse(lines.get(0).contains("\n"));
    }       
}
