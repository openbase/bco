/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.unit.core.consistency.device;

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
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
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
