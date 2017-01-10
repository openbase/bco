package org.openbase.bco.registry.unit.core.consistency.deviceconfig;

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
import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Map<String, String> labelConsistencyMap;

    public DeviceLabelConsistencyHandler() {
        this.labelConsistencyMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig deviceConfig = entry.getMessage();

        // update label
        if (!deviceConfig.hasLabel() | deviceConfig.getLabel().isEmpty()) {
            entry.setMessage(deviceConfig.toBuilder().setLabel(generateDeviceLabel(deviceConfig)));
            throw new EntryModification(entry, this);
        }

        // verify label
        checkUniqueness(deviceConfig);
    }

    private void checkUniqueness(final UnitConfig config) throws CouldNotPerformException {
        try {

            if (!config.hasId()) {
                throw new NotAvailableException("deviceconfig.id");
            }

            if (!config.hasLabel()) {
                throw new NotAvailableException("deviceconfig.label");
            }

            if (config.getLabel().isEmpty()) {
                throw new NotAvailableException("deviceconfig.label");
            }

            String deviceKey = generateKey(config);

            if (labelConsistencyMap.containsKey(deviceKey)) {
                if (!config.getId().equals(labelConsistencyMap.get(deviceKey))) {
                    throw new VerificationFailedException("Device[" + config.getId() + "] and Device[" + labelConsistencyMap.get(deviceKey) + "] are registerted with equal Label[" + config.getLabel() + "] on the same Location[" + config.getPlacementConfig().getLocationId() + "] which is not allowed!");
                }
            }

            labelConsistencyMap.put(generateKey(config), config.getId());

        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not check deviceconfig uniqueness of " + config + "!", ex);
        }
    }

    private String generateDeviceLabel(final UnitConfig config) throws CouldNotPerformException {
        try {

            if (config == null) {
                throw new NotAvailableException("deviceconfig");
            }
            if (!config.hasId()) {
                throw new NotAvailableException("deviceconfig.id");
            }
            if (config.getId().isEmpty()) {
                throw new NotAvailableException("Field deviceconfig.id is empty!");
            }
            return config.getId();
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not gernerate unit label!", ex);
        }
    }

    private String generateKey(UnitConfig config) throws NotAvailableException {

        if (!config.hasId()) {
            throw new NotAvailableException("deviceconfig.id");
        }

        if (!config.hasLabel()) {
            throw new NotAvailableException("deviceconfig.label");
        }

        if (config.getLabel().isEmpty()) {
            throw new NotAvailableException("field deviceconfig.label is empty");
        }

        if (!config.hasPlacementConfig()) {
            throw new NotAvailableException("deviceconfig.placementconfig");
        }

        if (!config.getPlacementConfig().hasLocationId() || config.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("deviceconfig.placementconfig.locationid");
        }

        return config.getPlacementConfig().getLocationId() + "_" + config.getLabel();
    }

    @Override
    public void reset() {
        labelConsistencyMap.clear();
    }
}
