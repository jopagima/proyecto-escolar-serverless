package com.jopagima.school.students.infrastructure.security;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.jopagima.school.students.infrastructure.observability.DenialReason;

/**
 * CallerGroups
 */
public class CallerGroups {
    private final static String GROUPS_CLAIM = "cognito:groups";
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

    public static CallerGroups from(APIGatewayV2HTTPEvent event) {
        String groupsClaimFromJwt = Optional.ofNullable(event.getRequestContext())
            .map(ctx -> ctx.getAuthorizer())
            .map(authorizer -> authorizer.getJwt())
            .map(jwt -> jwt.getClaims())
            .map(claims -> claims.get(GROUPS_CLAIM))
            .orElse("");
        return parse(groupsClaimFromJwt);
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    public DenialReason denialReason() {
          return (isEmpty()) ? DenialReason.NO_GROUPS : DenialReason.GROUP_NOT_ALLOWED;
    }

}
