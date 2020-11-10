package org.openbase.bco.api.graphql.schema;

import com.google.api.graphql.rejoiner.*;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import org.openbase.bco.api.graphql.AuthorizationContext;
import org.openbase.bco.api.graphql.coercing.GraphQLScalars;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType;
import org.openbase.type.domotic.authentication.AuthTokenType;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UnitSchemaModule extends SchemaModule {

    @SchemaModification
    TypeModification label = Type.find(UnitConfig.getDescriptor()).replaceField(
            GraphQLFieldDefinition.newFieldDefinition()
                    .name("label")
                    .type(GraphQLScalars.create())
                    .build());

    @Mutation("unit")
    ActionDescription getLamp(@Arg("alias") String alias, @Arg("power") String state, DataFetchingEnvironment env) throws InterruptedException, CouldNotPerformException, TimeoutException, ExecutionException {

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
        final String token = ((AuthorizationContext) env.getContext()).getToken();

        ColorableLightRemote unit = Units.getUnitByAlias(alias, true, Units.COLORABLE_LIGHT);
        PowerStateType.PowerState powerState = PowerStateType.PowerState.newBuilder().setValue(State.valueOf(state)).build();
        ActionParameterType.ActionParameter.Builder builder = ActionDescriptionProcessor.generateDefaultActionParameter(powerState, ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE, unit);
        builder.setAuthToken(AuthTokenType.AuthToken.newBuilder().setAuthenticationToken(token).build());
        return unit.applyAction(builder).get(5, TimeUnit.SECONDS);
    }
}
