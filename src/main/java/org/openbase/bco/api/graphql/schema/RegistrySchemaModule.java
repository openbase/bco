package org.openbase.bco.api.graphql.schema;

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

import com.google.api.graphql.rejoiner.*;
import com.google.common.collect.ImmutableList;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import org.openbase.bco.api.graphql.coercing.GraphQLScalars;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RegistrySchemaModule extends SchemaModule {

    /*@SchemaModification
    TypeModification label = Type.find(UnitConfig.getDescriptor()).replaceField(
            GraphQLFieldDefinition.newFieldDefinition()
                    .name("label")
                    .type(GraphQLScalars.create())
                    .build());*/

    @Query("unitConfig")
    UnitConfig getUnitConfigById(@Arg("id") String id) throws CouldNotPerformException, InterruptedException {
        return Registries.getUnitRegistry(true).getUnitConfigById(id);
    }

    @Query("unitConfigs")
    ImmutableList<UnitConfig> getUnitConfigs() throws CouldNotPerformException, InterruptedException {
        return ImmutableList.copyOf(Registries.getUnitRegistry(true).getUnitConfigs());
    }

    @Mutation("updateUnitConfig")
    UnitConfig updateUnitConfig(@Arg("unitConfig") UnitConfig unitConfig, DataFetchingEnvironment env) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        final UnitConfig.Builder builder = Registries.getUnitRegistry(true).getUnitConfigById(unitConfig.getId()).toBuilder();
        builder.mergeFrom(unitConfig);

        return Registries.getUnitRegistry(true).getUnitConfigById(unitConfig.getId());
    }

    @Mutation("removeUnitConfig")
    UnitConfig removeUnitConfig(@Arg("unitConfig") UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        return Registries.getUnitRegistry(true).removeUnitConfig(unitConfig).get(5, TimeUnit.SECONDS);
    }

    @Mutation("registerUnitConfig")
    UnitConfig registerUnitConfig(@Arg("unitConfig") UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        return Registries.getUnitRegistry(true).registerUnitConfig(unitConfig).get(5, TimeUnit.SECONDS);
    }

//    @Query("unitConfig") todo QueryType required in order to support multible arguments
//    UnitConfig getUnitConfigByAlias(@Arg("alias") String alias) throws CouldNotPerformException, InterruptedException {
//        return Registries.getUnitRegistry(true).getUnitConfigByAlias(alias);
//    }
}
