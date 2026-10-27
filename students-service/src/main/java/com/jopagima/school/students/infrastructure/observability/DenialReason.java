package com.jopagima.school.students.infrastructure.observability;

/**
 * DenialReason
 */
public enum DenialReason {

    GROUP_NOT_ALLOWED("GroupNotAllowed"), NO_GROUPS("NoGroups");

    private final String label;

    DenialReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
