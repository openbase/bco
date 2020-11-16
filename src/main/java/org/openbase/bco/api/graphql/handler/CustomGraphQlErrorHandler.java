package org.openbase.bco.api.graphql.handler;

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

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.servlet.core.GraphQLErrorHandler;
import org.openbase.bco.api.graphql.error.ArgumentError;
import org.openbase.bco.api.graphql.error.BCOGraphQLError;
import org.openbase.bco.api.graphql.error.ServerError;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class CustomGraphQlErrorHandler implements GraphQLErrorHandler {

    private final Logger logger = LoggerFactory.getLogger(GraphQLErrorHandler.class);

    @Override
    public List<GraphQLError> processErrors(List<GraphQLError> list) {
        final List<GraphQLError> errors = new ArrayList<>();
        for (final GraphQLError error : list) {
            if (!(error instanceof ExceptionWhileDataFetching)) {
                errors.add(error);
                continue;
            }

            final ExceptionWhileDataFetching dataFetchingError = (ExceptionWhileDataFetching) error;
            final Throwable cause = dataFetchingError.getException().getCause();

            // create as server error if the cause is not a bco graphql error
            final BCOGraphQLError bcoError = (cause instanceof BCOGraphQLError) ? ((BCOGraphQLError) cause) : new ServerError(dataFetchingError.getMessage(), cause);

            // update argument error fields
            if (bcoError instanceof ArgumentError) {
                System.out.println("Is argument error?");
                bcoError.setLocations(dataFetchingError.getLocations());
                bcoError.setPath(dataFetchingError.getPath());
            }

            if (bcoError instanceof ServerError) {
                ExceptionPrinter.printHistory(bcoError, logger);
            }

            errors.add(bcoError);
        }
        return errors;
    }
}
