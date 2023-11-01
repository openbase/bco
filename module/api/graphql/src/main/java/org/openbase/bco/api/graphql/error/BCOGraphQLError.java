package org.openbase.bco.api.graphql.error;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;

public abstract class BCOGraphQLError extends Exception implements GraphQLError {

    private List<Object> path = null;
    private List<SourceLocation> locations = null;

    public BCOGraphQLError(final Throwable cause) {
        super(cause.getMessage(), cause);
    }

    @Override
    public List<SourceLocation> getLocations() {
        return locations;
    }

    public void setLocations(final List<SourceLocation> locations) {
        this.locations = locations;
    }

    @Override
    public List<Object> getPath() {
        return path;
    }

    public void setPath(final List<Object> path) {
        this.path = path;
    }
}
