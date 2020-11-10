package org.openbase.bco.api.graphql;

import graphql.servlet.context.DefaultGraphQLContext;
import graphql.servlet.context.DefaultGraphQLServletContext;
import org.dataloader.DataLoaderRegistry;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthorizationContext extends DefaultGraphQLContext {

    private final String token;

    public AuthorizationContext(DataLoaderRegistry dataLoaderRegistry, String token) {
        super(dataLoaderRegistry, null);
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
