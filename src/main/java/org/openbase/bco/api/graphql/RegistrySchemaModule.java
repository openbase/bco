package org.openbase.bco.api.graphql;

import com.google.api.graphql.rejoiner.Arg;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLTypeReference;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

public class RegistrySchemaModule extends SchemaModule {

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
