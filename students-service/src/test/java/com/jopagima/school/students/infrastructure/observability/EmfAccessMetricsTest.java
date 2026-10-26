package com.jopagima.school.students.infrastructure.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
