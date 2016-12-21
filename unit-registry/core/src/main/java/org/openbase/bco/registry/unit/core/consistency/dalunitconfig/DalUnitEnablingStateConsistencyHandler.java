package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DalUnitEnablingStateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;
    private final Map<Boolean, Map<String, EnablingState>> oldHostEnablingStateMap;

    public DalUnitEnablingStateConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
        this.oldHostEnablingStateMap = new HashMap<>();
        this.oldHostEnablingStateMap.put(Boolean.TRUE, new HashMap<>());
        this.oldHostEnablingStateMap.put(Boolean.FALSE, new HashMap<>());
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig dalUnitConfig = entry.getMessage();

        if (!dalUnitConfig.hasUnitHostId() || dalUnitConfig.getUnitHostId().isEmpty()) {
            throw new NotAvailableException("unitConfig.unitHostId");
        }

        UnitConfig deviceUnitConfig = deviceRegistry.getMessage(dalUnitConfig.getUnitHostId());

        if (!oldHostEnablingStateMap.get(registry.isSandbox()).containsKey(dalUnitConfig.getId())) {
            oldHostEnablingStateMap.get(registry.isSandbox()).put(dalUnitConfig.getId(), deviceUnitConfig.getEnablingState());
        }

        if (oldHostEnablingStateMap.get(registry.isSandbox()).get(dalUnitConfig.getId()) != deviceUnitConfig.getEnablingState()) {
            oldHostEnablingStateMap.get(registry.isSandbox()).put(dalUnitConfig.getId(), deviceUnitConfig.getEnablingState());
            if (deviceUnitConfig.getEnablingState().getValue() == EnablingState.State.ENABLED) {
                throw new EntryModification(entry.setMessage(dalUnitConfig.toBuilder().setEnablingState(deviceUnitConfig.getEnablingState())), this);
            }
        }

        if (deviceUnitConfig.getEnablingState().getValue() == EnablingState.State.DISABLED) {
            if (dalUnitConfig.getEnablingState().getValue() == EnablingState.State.ENABLED) {
                EnablingState disabled = EnablingState.newBuilder().setValue(EnablingState.State.DISABLED).build();
                throw new EntryModification(entry.setMessage(dalUnitConfig.toBuilder().setEnablingState(disabled)), this);
            }
        }
    }

}
