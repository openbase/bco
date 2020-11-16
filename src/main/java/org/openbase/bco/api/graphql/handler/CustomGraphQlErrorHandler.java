package org.openbase.bco.api.graphql.handler;

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.servlet.core.GenericGraphQLError;
import graphql.servlet.core.GraphQLErrorHandler;
import graphql.validation.ValidationError;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.VariablePrinter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class CustomGraphQlErrorHandler implements GraphQLErrorHandler {

    @Override
    public List<GraphQLError> processErrors(List<GraphQLError> list) {

        final List<GraphQLError> errors = new ArrayList<>();
        for (GraphQLError error : list) {
            if(error instanceof ExceptionWhileDataFetching) {
                final Throwable exception = ((ExceptionWhileDataFetching) error).getException();

                if (exception instanceof CouldNotPerformException) {
                    final VariablePrinter variablePrinter = new VariablePrinter();
                    ExceptionPrinter.printHistory(exception, variablePrinter);
                    errors.add(new GenericGraphQLError(variablePrinter.getMessages()));
                    continue;
                }

                errors.add(new GenericGraphQLError("Internal Server Error - Please create a ticked at https://github.com/openbase/bco.api.graphql/issues/new and provide the following information: "
                        + "Exception["+error.getClass()+": "+error.getMessage()));
                continue;
            }

            // default case
            errors.add(error);

//            if(error instanceof ValidationError) {
//                errors.add(new GenericGraphQLError("Invalid query: "+ error.getMessage()));
//                continue;
//            }

        }
        return errors;
    }
}