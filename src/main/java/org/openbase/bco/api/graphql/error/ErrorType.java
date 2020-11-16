package org.openbase.bco.api.graphql.error;

import graphql.ErrorClassification;

public enum ErrorType implements ErrorClassification {

    ARGUMENT_ERROR,
    AUTHORIZATION_ERROR,
    SERVER_ERROR;

    private ErrorType() {
    }
}
