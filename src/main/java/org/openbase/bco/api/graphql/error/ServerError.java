package org.openbase.bco.api.graphql.error;

import graphql.ErrorClassification;

public class ServerError extends BCOGraphQLError {

    public ServerError(String message) {
        this(message, null);
    }

    public ServerError(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorType.SERVER_ERROR;
    }
}
