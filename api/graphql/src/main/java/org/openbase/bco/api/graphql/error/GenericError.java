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
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;

import javax.crypto.BadPaddingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

public class GenericError extends BCOGraphQLError {

    private final Throwable initialCause;

    public GenericError(final Throwable cause) {
        super(ExceptionProcessor.getInitialCauseMessage(cause), ExceptionProcessor.getInitialCause(cause));
        initialCause = ExceptionProcessor.getInitialCause(getCause());
    }

    @Override
    public ErrorClassification getErrorType() {

        // skip if no cause has been set.
        if (initialCause == null) {
            return ErrorType.SERVER_ERROR;
        }

        // compute error type based on initial cause
        if (initialCause instanceof TimeoutException
                | initialCause instanceof org.openbase.jul.exception.TimeoutException
                | initialCause instanceof InterruptedException
                | initialCause instanceof FatalImplementationErrorException
                | initialCause instanceof NotInitializedException
                | initialCause instanceof InstantiationException
                | initialCause instanceof InvocationFailedException
                | initialCause instanceof ShutdownException
                | initialCause instanceof ExecutionException) {
            return ErrorType.SERVER_ERROR;
        } else if (initialCause instanceof NotAvailableException
                | initialCause instanceof VerificationFailedException
                | initialCause instanceof InvalidStateException
                | initialCause instanceof IllegalArgumentException
                | initialCause instanceof NotSupportedException
                | initialCause instanceof TypeNotSupportedException
                | initialCause instanceof RejectedExecutionException) {
            return ErrorType.ARGUMENT_ERROR;
        } else if (initialCause instanceof PermissionDeniedException
                | initialCause instanceof BadPaddingException) {
            return ErrorType.AUTHORIZATION_ERROR;
        } else {
            return ErrorType.SERVER_ERROR;
        }
    }
}
