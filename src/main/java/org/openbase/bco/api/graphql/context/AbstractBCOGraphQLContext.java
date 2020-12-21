package org.openbase.bco.api.graphql.context;

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

import graphql.kickstart.execution.context.DefaultGraphQLContext;
import org.dataloader.DataLoaderRegistry;
import org.openbase.jul.exception.NotAvailableException;

import javax.security.auth.Subject;

public abstract class AbstractBCOGraphQLContext extends DefaultGraphQLContext {

    public static final String DATA_LOADER_UNITS = "units";

    public AbstractBCOGraphQLContext(DataLoaderRegistry dataLoaderRegistry, Subject subject) {
        super(dataLoaderRegistry, subject);
    }

    public abstract String getToken() throws NotAvailableException;

    public abstract String getLanguageCode();
}
