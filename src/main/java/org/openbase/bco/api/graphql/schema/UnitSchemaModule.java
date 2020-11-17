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
import com.google.protobuf.Message;
import graphql.schema.DataFetchingEnvironment;
import org.openbase.bco.api.graphql.BCOGraphQLContext;
import org.openbase.bco.api.graphql.error.ArgumentError;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ProtoBufBuilderProcessor;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UnitSchemaModule extends SchemaModule {


    @SchemaModification(addField = "config", onType = LocationData.class)
    UnitConfig addConfigToData(LocationData data) throws NotAvailableException {
        return Registries.getUnitRegistry().getUnitConfigById(data.getId());
    }

    @Query("unit")
    LocationData unit(@Arg("unitId") String id) throws InterruptedException, ArgumentError {
        try {
            final UnitRemote<?> unit = Units.getUnit(id, true);
            return (LocationData) ProtoBufBuilderProcessor.merge(LocationData.newBuilder(), unit.getData()).build();
        } catch (NotAvailableException ex) {
            throw new ArgumentError(ex);
        }
    }

    private List<UnitConfig> getUnitConfigsByFilter(UnitFilter unitFilter) throws CouldNotPerformException, InterruptedException {
        // setup default values
        if ((unitFilter == null)) {
            unitFilter = UnitFilter.getDefaultInstance();
        }

        return new RegistrySchemaModule.UnitFilterImpl(unitFilter)
                .pass(Registries.getUnitRegistry(true).getUnitConfigsFiltered(true));
    }

    @Query("units")
    ImmutableList<LocationData> units(@Arg("filter") UnitFilter unitFilter) throws CouldNotPerformException, InterruptedException {
        List<LocationData> dataList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigsByFilter(unitFilter)) {
            UnitRemote<?> unit = Units.getUnit(unitConfig, true);
            dataList.add((LocationData) ProtoBufBuilderProcessor.merge(LocationData.newBuilder(), unit.getData()).build());
        }

        return ImmutableList.copyOf(dataList);
    }

    private LocationData setServiceStates(final UnitConfig unitConfig, final LocationData data, DataFetchingEnvironment env) throws CouldNotPerformException, InterruptedException {
        return setServiceStates(unitConfig.getId(), data, env);
    }

    private LocationData setServiceStates(final String unitId, final LocationData data, DataFetchingEnvironment env) throws CouldNotPerformException, InterruptedException {
        final UnitRemote<?> unit = Units.getUnit(unitId, true);

        final List<RemoteAction> remoteActions = new ArrayList<>();
        for (final ServiceType serviceType : unit.getSupportedServiceTypes()) {
            if (!Services.hasServiceState(serviceType, ServiceTempus.CURRENT, data)) {
                continue;
            }

            Message serviceState = Services.invokeProviderServiceMethod(serviceType, data);

            ActionParameter.Builder builder = ActionDescriptionProcessor.generateDefaultActionParameter(serviceState, serviceType);
            try {
                builder.setAuthToken(AuthToken.newBuilder().setAuthenticationToken(((BCOGraphQLContext) env.getContext()).getToken()).build());
            } catch (NotAvailableException ex) {
                // in case the auth token is not available, we just continue without any authentication.
            }
            remoteActions.add(new RemoteAction(unit.applyAction(builder)));
        }

        // TODO: blocked by https://github.com/openbase/bco.dal/issues/170
        if (!remoteActions.isEmpty()) {
            for (final RemoteAction remoteAction : remoteActions) {
                remoteAction.waitForRegistration(5, TimeUnit.SECONDS);
            }
        }

        return (LocationData) ProtoBufBuilderProcessor.merge(LocationData.newBuilder(), unit.getData()).build();
    }

    @Mutation("unit")
    LocationData unit(@Arg("unitId") String unitId, @Arg("data") LocationData data, DataFetchingEnvironment env) throws CouldNotPerformException, InterruptedException {
        return setServiceStates(unitId, data, env);
    }

    @Mutation("units")
    ImmutableList<LocationData> units(@Arg("filter") UnitFilter filter, @Arg("data") LocationData data, DataFetchingEnvironment env) throws CouldNotPerformException, InterruptedException {
        final List<LocationData> dataList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigsByFilter(filter)) {
            dataList.add(setServiceStates(unitConfig, data, env));
        }
        return ImmutableList.copyOf(dataList);
    }
}
