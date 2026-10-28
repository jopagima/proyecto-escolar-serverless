package com.jopagima.school.students.infrastructure;

import java.util.ArrayList;
import java.util.List;


import com.jopagima.school.students.infrastructure.observability.AccessMetrics;
import com.jopagima.school.students.infrastructure.observability.DenialReason;

/**
 * RecordingAccessMetrics
 */
public class RecordingAccessMetrics implements AccessMetrics {

    List<String> denials = new ArrayList<>();


    @Override
    public void recordDenied(String operation, DenialReason reason) {
            String line =   operation + ":" + reason.getLabel();
            denials.add(line);
    }

}
