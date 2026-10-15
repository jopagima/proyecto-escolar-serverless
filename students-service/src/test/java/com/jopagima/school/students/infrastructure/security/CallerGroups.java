package com.jopagima.school.students.infrastructure.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CallerGroups
 */
public class CallerGroups {
    private final Set<String> groups;

    private CallerGroups(Set<String> groups) {
        this.groups = groups;
    }

	public static CallerGroups parse(String claimValue) {
        if (claimValue == null || claimValue.isEmpty()) {
            return new CallerGroups(Set.of());
        }
        if (!claimValue.startsWith("[") || !claimValue.endsWith("]")) {
            return new CallerGroups(Set.of());
        }

        String inner = claimValue.substring(1, claimValue.length() - 1);
        Set<String> groups = Arrays.stream(inner.split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        return new CallerGroups(groups);
    }

    public boolean hasAnyOf(String... allowedGroups) {
        for (String group : allowedGroups) {
            if (groups.contains(group)) {
                return true;
            }
        }
        return false;
    }

}
