package org.openbase.bco.api.graphql;

import com.google.api.graphql.rejoiner.*;
import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import org.openbase.bco.api.graphql.coercing.GraphQLScalars;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.rmi.server.UnicastRemoteObject;

public class RegistrySchemaModule extends SchemaModule {

        @SchemaModification
        TypeModification label = Type.find(UnitConfig.getDescriptor()).replaceField(
                GraphQLFieldDefinition.newFieldDefinition()
                        .name("label")
                        .type(GraphQLScalars.create())
                .build());

    @Query("unit")
    UnitConfig getUnit(@Arg("id") String id) {
        try {
            return Registries.getUnitRegistry(true).getUnitConfigById(id);
        } catch (CouldNotPerformException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return UnitConfig.getDefaultInstance();
    }
}
