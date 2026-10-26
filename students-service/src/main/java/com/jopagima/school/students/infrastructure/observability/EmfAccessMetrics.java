package com.jopagima.school.students.infrastructure.observability;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * EmfAccessMetrics
 */
public class EmfAccessMetrics implements AccessMetrics{


    private final Consumer<String> sink;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmfAccessMetrics(Consumer<String> sink) {
        this.sink = sink;
    }

    public static EmfAccessMetrics toStandardOutput() {
        return new EmfAccessMetrics(System.out::println);
    }

    @Override 
    public void recordDenied(String operation, DenialReason reason) {

        try {
            String line = objectMapper.writeValueAsString(Map.of(
                "_aws", Map.of(
                    "Timestamp", System.currentTimeMillis(),
                    "CloudWatchMetrics", List.of(
                        Map.of(
                            "Namespace", "SchoolPlatform/Access",
                            "Dimensions", List.of(List.of("Operation", "Reason")),
                            "Metrics", List.of(Map.of("Name", "AccessDenied", "Unit", "Count"))
                        )
                    )
                ),
                "Operation", operation,
                "Reason", reason.getLabel(),
                "AccessDenied", 1
            ));
            sink.accept(line);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize access metric", e);
        }
    }

}
