package org.openbase.bco.api.graphql.schema;

import com.google.api.graphql.rejoiner.*;
import graphql.schema.GraphQLFieldDefinition;
import org.openbase.bco.api.graphql.coercing.GraphQLScalars;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

public class RegistrySchemaModule extends SchemaModule {

    @SchemaModification
    TypeModification label = Type.find(UnitConfig.getDescriptor()).replaceField(
            GraphQLFieldDefinition.newFieldDefinition()
                    .name("label")
                    .type(GraphQLScalars.create())
                    .build());

    @Query("unitConfig")
    UnitConfig getUnitConfigById(@Arg("id") String id) throws CouldNotPerformException, InterruptedException {
        return Registries.getUnitRegistry(true).getUnitConfigById(id);
    }

//    @Query("unitConfig") todo QueryType required in order to support multible arguments
//    UnitConfig getUnitConfigByAlias(@Arg("alias") String alias) throws CouldNotPerformException, InterruptedException {
//        return Registries.getUnitRegistry(true).getUnitConfigByAlias(alias);
//    }
}
