package org.openbase.bco.registry.unit.core.consistency.locationconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationUnitIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> agentRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> sceneRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitGroupRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userRegistry;

    public LocationUnitIdConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> agentRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> scneRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitGroupRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userRegistry) {
        this.agentRegistry = agentRegistry;
        this.appRegistry = appRegistry;
        this.authorizationGroupRegistry = authorizationGroupRegistry;
        this.connectionRegistry = connectionRegistry;
        this.dalUnitRegistry = dalUnitRegistry;
        this.deviceRegistry = deviceRegistry;
        this.sceneRegistry = scneRegistry;
        this.unitGroupRegistry = unitGroupRegistry;
        this.userRegistry = userRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder locationUnitConfig = entry.getMessage().toBuilder();
        LocationConfig.Builder locationConfig = locationUnitConfig.getLocationConfigBuilder();

        Collection<String> lookupUnitIds;

        try {
            lookupUnitIds = lookupUnitIds(entry.getMessage(), registry);
        } catch (CouldNotPerformException ex) {
            lookupUnitIds = new ArrayList<>();
        }

        // verify and update unit ids
        if (generateListHash(locationConfig.getUnitIdList()) != generateListHash(lookupUnitIds)) {
            locationConfig.clearUnitId().addAllUnitId(lookupUnitIds);
            entry.setMessage(locationUnitConfig);
            throw new EntryModification(entry, this);
        }
    }

    private Collection<String> lookupUnitIds(final UnitConfig locationUnitConfig, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry) throws CouldNotPerformException {
        try {
            if (!locationUnitConfig.hasId() || locationUnitConfig.getId().isEmpty()) {
                throw new NotAvailableException("location id");
            }

            Set<String> unitIdList = new HashSet<>();
            List<UnitConfig> allUnits = new ArrayList<>();
            allUnits.addAll(agentRegistry.getMessages());
            allUnits.addAll(appRegistry.getMessages());
            allUnits.addAll(authorizationGroupRegistry.getMessages());
            allUnits.addAll(connectionRegistry.getMessages());
            allUnits.addAll(dalUnitRegistry.getMessages());
            allUnits.addAll(deviceRegistry.getMessages());
            allUnits.addAll(sceneRegistry.getMessages());
            allUnits.addAll(unitGroupRegistry.getMessages());
            allUnits.addAll(userRegistry.getMessages());
            for (UnitConfig unitConfig : allUnits) {

                // skip units with no placenment config
                if (!unitConfig.hasPlacementConfig()) {
                    continue;
                } else if (!unitConfig.getPlacementConfig().hasLocationId()) {
                    continue;
                } else if (unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                    continue;
                }

                // filter units which are currently diabled
                if (!unitConfig.hasEnablingState() || !unitConfig.getEnablingState().hasValue()) {
                    continue;
                } else if (unitConfig.getEnablingState().getValue() != EnablingStateType.EnablingState.State.ENABLED) {
                    continue;
                }

                // add direct assigned units
                if (unitConfig.getPlacementConfig().getLocationId().equals(locationUnitConfig.getId())) {
                    unitIdList.add(unitConfig.getId());
                }

            }

            // add child units
            for (String childId : locationUnitConfig.getLocationConfig().getChildIdList()) {
                unitIdList.addAll(locationRegistry.get(childId).getMessage().getLocationConfig().getUnitIdList());
            }
            return unitIdList;
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Unit id loockup failed!", ex);
        }
    }

    private int generateListHash(Collection<String> list) {
        int hash = 0;
        hash = list.stream().map((entry) -> entry.hashCode()).reduce(hash, Integer::sum);
        return hash;
    }
}
