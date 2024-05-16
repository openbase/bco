package org.openbase.bco.api.graphql.error

import graphql.ErrorClassification
import java.util.concurrent.TimeUnit

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
 */   class ServerError : BCOGraphQLError {
    @JvmOverloads
    constructor(message: String?, cause: Throwable) : super(cause)
    constructor(cause: Throwable) : super(cause)

    override fun getErrorType(): ErrorClassification {
        return ErrorType.SERVER_ERROR
    }

    companion object {
        const val BCO_TIMEOUT_SHORT: Long = 10
        const val BCO_TIMEOUT_LONG: Long = 20
        val BCO_TIMEOUT_TIME_UNIT = TimeUnit.SECONDS
    }
}
