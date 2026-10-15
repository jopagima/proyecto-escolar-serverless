package com.jopagima.school.students.infrastructure.security;

import java.util.Set;

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
        
        String[] groups = claimValue.substring(1, claimValue.length() - 1).split(" ");
        return new CallerGroups(Set.of(groups));
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
