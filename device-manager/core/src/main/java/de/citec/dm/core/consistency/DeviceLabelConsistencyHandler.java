/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.HashMap;
import java.util.Map;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceLabelConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final Map<String, String> labelConsistencyMap;

    public DeviceLabelConsistencyHandler() {
        this.labelConsistencyMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig deviceConfig = entry.getMessage();

        // update label
        if (!deviceConfig.hasLabel() | deviceConfig.getLabel().isEmpty()) {
            entry.setMessage(deviceConfig.toBuilder().setLabel(generateDeviceLabel(deviceConfig)));
            throw new EntryModification(entry, this);
        }

        // verify label
        checkUniqueness(deviceConfig);
    }

    private void checkUniqueness(final DeviceConfig config) throws CouldNotPerformException {
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

    private String generateDeviceLabel(final DeviceConfig config) throws CouldNotPerformException {
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

    private String generateKey(DeviceConfig config) throws NotAvailableException {

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
