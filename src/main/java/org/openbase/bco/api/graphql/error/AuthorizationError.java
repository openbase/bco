package org.openbase.bco.api.graphql.error;

import graphql.ErrorClassification;

public class AuthorizationError extends BCOGraphQLError {

    public AuthorizationError(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorType.AUTHORIZATION_ERROR;
    }
}
