package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.lib.util.DeviceConfigUtils;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.unit.core.consistency.DefaultUnitLabelConsistencyHandler;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.type.language.LabelType.Label;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DalUnitLabelConsistencyHandler extends DefaultUnitLabelConsistencyHandler {

    private final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry;
    private final Map<String, Label> oldUnitHostLabelMap;

    public DalUnitLabelConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry,
                                          final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry)
            throws InstantiationException {
        super();

        try {
            this.deviceClassRegistry = CachedClassRegistryRemote.getRegistry().getDeviceClassRemoteRegistry();
            this.deviceRegistry = deviceRegistry;
            this.appRegistry = appRegistry;
            this.oldUnitHostLabelMap = new HashMap<>();
        } catch (final CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void processData(final String id,
                            final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry,
                            final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap,
                            final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry)
            throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();
        // virtual units will be handles by super call
        if (!UnitConfigProcessor.isVirtualUnit(dalUnitConfig)) {
            if (!UnitConfigProcessor.isHostUnitAvailable(dalUnitConfig)) {
                throw new NotAvailableException("unitConfig.unitHostId");
            }

            if (deviceRegistry.contains(dalUnitConfig.getUnitHostId())) {
                // handle if host unit is a device
                UnitConfig hostUnitConfig = deviceRegistry.getMessage(dalUnitConfig.getUnitHostId());
                DeviceClass deviceClass = deviceClassRegistry.get(hostUnitConfig.getDeviceConfig().getDeviceClassId()).getMessage();

                if (!oldUnitHostLabelMap.containsKey(dalUnitConfig.getId())) {
                    oldUnitHostLabelMap.put(dalUnitConfig.getId(), hostUnitConfig.getLabel());
                }

                boolean hasDuplicatedUnitType = DeviceConfigUtils.checkDuplicatedUnitType(hostUnitConfig, deviceClass, registry);

                // Setup device label if unit has no label configured.
                if (!dalUnitConfig.hasLabel() || LabelProcessor.isEmpty(dalUnitConfig.getLabel())) {
                    if (DeviceConfigUtils.setupUnitLabelByDeviceConfig(dalUnitConfig, hostUnitConfig, deviceClass, hasDuplicatedUnitType)) {
                        throw new EntryModification(entry.setMessage(dalUnitConfig, this), this);
                    }
                }

                Label oldLabel = oldUnitHostLabelMap.get(dalUnitConfig.getId());
                if (!oldLabel.equals(hostUnitConfig.getLabel())) {
                    // host label has changed
                    logger.debug("Host label has changed from [" + oldLabel + "] to [" + hostUnitConfig.getLabel() + "] for unit [" + dalUnitConfig.getAlias(0) + "]");
                    oldUnitHostLabelMap.put(dalUnitConfig.getId(), hostUnitConfig.getLabel());
                    if (dalUnitConfig.getLabel().equals(oldLabel)) {
                        // dal unit label is still the same
                        throw new EntryModification(entry.setMessage(dalUnitConfig.setLabel(hostUnitConfig.getLabel()), this), this);
                    }
                }
            } else if (appRegistry.contains(dalUnitConfig.getUnitHostId())) {
                // handle if host unit is a app
                UnitConfig hostUnitConfig = appRegistry.getMessage(dalUnitConfig.getUnitHostId());

                if (!oldUnitHostLabelMap.containsKey(dalUnitConfig.getId())) {
                    oldUnitHostLabelMap.put(dalUnitConfig.getId(), hostUnitConfig.getLabel());
                }

                // we do not have a strategy here yet since no example exists
                // just let super setup the alias as a default label
            }
        }

        // make sure that label exists and are unique per location per unit type
        super.processData(id, entry, entryMap, registry);
    }

    /**
     * Make sure that the label is unique per unit type and per location.
     *
     * @param label      the label for which the key is generated
     * @param unitConfig the unit having the label
     * @return a key unique per unit type per location
     */
    @Override
    protected String generateKey(String label, UnitConfig unitConfig) {
        return label + unitConfig.getUnitType().name() + unitConfig.getPlacementConfig().getLocationId();
    }
}
