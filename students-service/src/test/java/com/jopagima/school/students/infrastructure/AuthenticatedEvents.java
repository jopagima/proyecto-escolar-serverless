package com.jopagima.school.students.infrastructure;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

/**
 * AuthenticatedEvents
 */
public class AuthenticatedEvents {

    

    private AuthenticatedEvents() {
    }

    public static APIGatewayV2HTTPEvent withGroups(String groupsClaim) {
        APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT jwt =
                new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT();
        jwt.setClaims(Map.of("cognito:groups", groupsClaim));
        APIGatewayV2HTTPEvent.RequestContext.Authorizer authorizer =
                new APIGatewayV2HTTPEvent.RequestContext.Authorizer();
        authorizer.setJwt(jwt);
        APIGatewayV2HTTPEvent.RequestContext context = new APIGatewayV2HTTPEvent.RequestContext();
        context.setAuthorizer(authorizer);
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRequestContext(context);
        return event;
    }

}
