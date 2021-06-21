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

import graphql.ErrorClassification;

import java.util.concurrent.TimeUnit;

public class ServerError extends BCOGraphQLError {

    public static final long BCO_TIMEOUT_SHORT = 10;
    public static final long BCO_TIMEOUT_LONG = 20;
    public static final TimeUnit BCO_TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;

    public ServerError(String message) {
        this(message, null);
    }

    public ServerError(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerError(final Throwable cause) {
        super(cause.getMessage(), cause);
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorType.SERVER_ERROR;
    }
}
