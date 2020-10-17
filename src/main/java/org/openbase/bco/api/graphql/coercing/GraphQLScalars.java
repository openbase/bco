package org.openbase.bco.api.graphql.coercing;

import graphql.schema.GraphQLScalarType;

public class GraphQLScalars {

    public static GraphQLScalarType create() {
        return GraphQLScalarType.newScalar()
                .name("UnitLabelScaler")
                .coercing(new LabelTypeCoercing())
                .description("Convert label...").build();
    }
}
