package org.openbase.bco.api.graphql;

import com.google.api.graphql.rejoiner.*;
import graphql.schema.GraphQLFieldDefinition;
import org.openbase.bco.api.graphql.coercing.GraphQLScalars;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    @Mutation("lamp")
    ActionDescription getLamp(@Arg("alias") String alias, @Arg("power") String state) throws InterruptedException, NotAvailableException, TimeoutException, ExecutionException {

//        final State state1 = State.valueOf(state);
//        try {
//            Futures.
//            return CompleFutureProcessor.toCompletableFuture(Units.getUnitByAlias(alias, true, Units.COLORABLE_LIGHT).setPowerState(state1));
//        } catch (NotAvailableException | ExecutionException | TimeoutException e) {
//            throw e;
//        } catch (InterruptedException e) {
//            throw e;
//        }
//    }

        final State state1 = State.valueOf(state);
            try {
                return Units.getUnitByAlias(alias, true, Units.COLORABLE_LIGHT).setPowerState(state1).get(5, TimeUnit.SECONDS);
            } catch (NotAvailableException | ExecutionException | TimeoutException e) {
                throw e;
            } catch (InterruptedException e) {
               throw e;
            }
        }
    }
