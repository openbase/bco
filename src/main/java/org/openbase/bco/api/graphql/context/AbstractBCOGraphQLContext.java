package org.openbase.bco.api.graphql.context;

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
