/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.unit.core.consistency.device;

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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.state.EnablingStateType.EnablingState;
import rst.homeautomation.state.InventoryStateType.InventoryState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceEnablingStateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig deviceUnitConfig = entry.getMessage();
        DeviceConfig deviceConfig = deviceUnitConfig.getDeviceConfig();

        if (!deviceConfig.hasInventoryState()) {
            throw new NotAvailableException("deviceConfig.inventoryState");
        }

        if (!deviceConfig.getInventoryState().hasValue()) {
            throw new NotAvailableException("deviceConfig.inventoryState.value");
        }

        if (deviceConfig.getInventoryState().getValue() != InventoryState.State.INSTALLED) {
            if (deviceUnitConfig.getEnablingState().getValue() == EnablingState.State.ENABLED) {
                EnablingState disabled = EnablingState.newBuilder().setValue(EnablingState.State.DISABLED).build();
                throw new EntryModification(entry.setMessage(deviceUnitConfig.toBuilder().setEnablingState(disabled)), this);
            }
        }
    }

}
