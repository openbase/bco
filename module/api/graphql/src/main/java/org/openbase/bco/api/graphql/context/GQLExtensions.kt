package org.openbase.bco.api.graphql.context

import graphql.schema.DataFetchingEnvironment

val DataFetchingEnvironment.context get() = (this.getContext() as AbstractBCOGraphQLContext)
