package org.openbase.bco.api.graphql.subscriptions;

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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.reactivex.BackpressureStrategy;
import org.openbase.bco.api.graphql.error.BCOGraphQLError;
import org.openbase.bco.api.graphql.error.GenericError;
import org.openbase.bco.api.graphql.error.ServerError;
import org.openbase.bco.api.graphql.schema.RegistrySchemaModule;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.ProtoBufBuilderProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.registry.UnitRegistryDataType;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitDataType;
import org.openbase.type.domotic.unit.UnitFilterType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SubscriptionModule {

    //TODO: what is a good value here
    private static BackpressureStrategy BACKPRESSURE_STRATEGY = BackpressureStrategy.BUFFER;

    public static Publisher<UnitDataType.UnitData> subscribeUnits(UnitFilterType.UnitFilter unitFilter) throws BCOGraphQLError {
        System.out.println("Subscribe to units");
        try {
            /*
            ColorableLightRemote unit = Units.getUnit(Registries.getUnitRegistry().getUnitConfigByAlias("ColorableLight-38"), false, ColorableLightRemote.class);
            return AbstractObserverMapper.createObservable(
                    unit::addDataObserver,
                    unit::removeDataObserver,
                    new AbstractObserverMapper<DataProvider<ColorableLightDataType.ColorableLightData>, ColorableLightDataType.ColorableLightData, UnitDataType.UnitData>() {

                        @Override
                        public UnitDataType.UnitData mapData(ColorableLightDataType.ColorableLightData data) throws Exception {
                            return (UnitDataType.UnitData) ProtoBufBuilderProcessor.merge(UnitDataType.UnitData.newBuilder(), data).build();
                        }
                    }
            ).toFlowable(BACKPRESSURE_STRATEGY);
             */

            //TODO: this never triggers any updates -- the custom unit pool itself does not
            final CustomUnitPool subscriptionUnitPool = new CustomUnitPool();
            subscriptionUnitPool.init(RegistrySchemaModule.buildUnitConfigFilter(unitFilter));

            return AbstractObserverMapper.createObservable(
                    subscriptionUnitPool::addObserver,
                    subscriptionUnitPool::removeObserver,
                    new AbstractObserverMapper<ServiceStateProvider<Message>, Message, UnitDataType.UnitData>() {
                        @Override
                        public UnitDataType.UnitData mapData(Message data) throws Exception {
                            System.out.println("Received data: " + data.getClass().getSimpleName());
                            UnitDataType.UnitData.Builder builder = UnitDataType.UnitData.newBuilder();
                            builder.getPowerStateBuilder().setValue(PowerStateType.PowerState.State.ON);
                            return builder.build();
                        }

                        @Override
                        public void doAfterAddObserver() throws CouldNotPerformException, InterruptedException {
                            System.out.println("Activate unit subscription pool");
                            subscriptionUnitPool.activate();
                        }

                        @Override
                        public void doAfterRemoveObserver() {
                            // todo: subscription service shutdown needs to be implemented.
                            //subscriptionUnitPool.shutdown();
                        }
                    }).toFlowable(BACKPRESSURE_STRATEGY);
        } catch (RuntimeException | CouldNotPerformException | InterruptedException ex) {
            throw new GenericError(ex);
        }
    }

    public static Publisher<UnitConfigType.UnitConfig> subscribeUnitConfigs(final UnitFilterType.UnitFilter unitFilter, boolean includeDisabledUnits) throws BCOGraphQLError {
        System.out.println("Subscribe to registry");
        return null;
        /*try {
            UnitRegistryRemote unitRegistry = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT);

            final ArrayList<UnitConfigType.UnitConfig> unitConfigs = new ArrayList<>(RegistrySchemaModule.getUnitConfigs(unitFilter, includeDisabledUnits));
            return AbstractObserverMapper.createObservable(unitRegistry::addDataObserver, unitRegistry::removeDataObserver,
                    new AbstractObserverMapper<DataProvider<UnitRegistryDataType.UnitRegistryData>, UnitRegistryDataType.UnitRegistryData, List<UnitConfigType.UnitConfig>>() {

                        @Override
                        public List<UnitConfigType.UnitConfig> mapData(UnitRegistryDataType.UnitRegistryData data) throws Exception {
                            System.out.println("Map data...");
                            final ImmutableList<UnitConfigType.UnitConfig> newUnitConfigs = RegistrySchemaModule.getUnitConfigs(unitFilter, includeDisabledUnits);
                            if(newUnitConfigs.equals(unitConfigs)) {
                                // nothing has changed
                                return;
                            }

                            // store update
                            unitConfigs.clear();
                            unitConfigs.addAll(newUnitConfigs);

                            return unitConfigs;
                        }
                    }).toFlowable(BACKPRESSURE_STRATEGY);
        } catch (RuntimeException | CouldNotPerformException | InterruptedException ex) {
            throw new GenericError(ex);
        }*/
    }
}
