package org.openbase.bco.api.graphql.error

import graphql.ErrorClassification
import org.openbase.jul.exception.*
import org.openbase.jul.exception.ExceptionProcessor.getInitialCause
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeoutException
import javax.crypto.BadPaddingException


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
class GenericError(
    cause: Throwable,
) : BCOGraphQLError(getInitialCause(cause)) {
    override fun getErrorType(): ErrorClassification = when (cause) {
        is TimeoutException,
        is org.openbase.jul.exception.TimeoutException,
        is InterruptedException,
        is FatalImplementationErrorException,
        is NotInitializedException,
        is InstantiationException,
        is InvocationFailedException,
        is ShutdownException,
        is java.util.concurrent.ExecutionException,
        -> ErrorType.SERVER_ERROR

        is NotAvailableException,
        is VerificationFailedException,
        is InvalidStateException,
        is java.lang.IllegalArgumentException,
        is NotSupportedException,
        is TypeNotSupportedException,
        is RejectedExecutionException,
        -> ErrorType.ARGUMENT_ERROR

        is PermissionDeniedException,
        is BadPaddingException,
        -> ErrorType.AUTHORIZATION_ERROR

        else -> ErrorType.SERVER_ERROR
    }
}
