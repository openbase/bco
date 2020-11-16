package org.openbase.bco.api.graphql.error;

import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;

public abstract class BCOGraphQLError extends Throwable implements GraphQLError {

    private List<Object> path = null;
    private List<SourceLocation> locations = null;

    public BCOGraphQLError(final String message, final Throwable cause) {
        super(message, cause);
    }

    @Override
    public List<SourceLocation> getLocations() {
        return locations;
    }

    @Override
    public List<Object> getPath() {
        return path;
    }

    public void setLocations(final List<SourceLocation> locations) {
        this.locations = locations;
    }

    public void setPath(final List<Object> path) {
        this.path = path;
    }
}
