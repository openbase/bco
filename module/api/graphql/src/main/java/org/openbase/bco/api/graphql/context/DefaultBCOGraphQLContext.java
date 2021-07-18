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
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class DefaultBCOGraphQLContext extends AbstractBCOGraphQLContext {

    private final String token;
    private final String languageCode;

    public DefaultBCOGraphQLContext(DataLoaderRegistry dataLoaderRegistry, Subject subject, HttpServletRequest request) {
        super(dataLoaderRegistry, subject);
        this.token = request.getHeader("Authorization");

        final String language = request.getHeader("Accept-Language");
        this.languageCode = (language != null) ? language : Locale.getDefault().getLanguage();
    }

    public String getToken() throws NotAvailableException {
        if (token == null) {
            throw new NotAvailableException("AuthToken");
        }

        return token;
    }

    public String getLanguageCode() {
        return this.languageCode;
    }
}
