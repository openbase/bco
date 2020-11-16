package org.openbase.bco.api.graphql.error;

import graphql.ErrorClassification;

public class ArgumentError extends BCOGraphQLError {

    public ArgumentError(final Throwable cause) {
        super(cause.getMessage(), cause);
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorType.ARGUMENT_ERROR;
    }


}
