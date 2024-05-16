package org.openbase.bco.api.graphql.context

import jakarta.servlet.http.HttpServletRequest
import org.dataloader.DataLoaderRegistry
import java.util.*

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
class DefaultBCOGraphQLContext(
    dataLoaderRegistry: DataLoaderRegistry,
    private val request: HttpServletRequest,
) : AbstractBCOGraphQLContext(dataLoaderRegistry) {
    override val token: String?
        get() = request.getHeader("Authorization")
    override val languageCode: String?
        get() = request.getHeader("Accept-Language") ?: Locale.getDefault().language
}
