/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.processing.StringProcessor;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import de.citec.lm.remote.LocationRegistryRemote;
import rst.configuration.EntryType.Entry;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.homeautomation.binding.BindingTypeHolderType.BindingTypeHolder.BindingType;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author thuxohl
 */
public class OpenhabServiceConfigItemIdConsistenyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";
    public static final String OPENHAB_BINDING_ITEM_ID = "OPENHAB_BINDING_ITEM_ID";

    private final LocationRegistryRemote locationRegistryRemote;
    private final ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry;

    public OpenhabServiceConfigItemIdConsistenyHandler(final LocationRegistryRemote locationRegistryRemote, ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry) {
        this.locationRegistryRemote = locationRegistryRemote;
        this.deviceClassRegistry = deviceClassRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        deviceConfig.clearUnitConfig();
        boolean modification = false;
        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {
            UnitConfig.Builder unitConfigClone = unitConfig.clone();
            unitConfig.clearServiceConfig();
            for (ServiceConfig.Builder serviceConfig : unitConfigClone.getServiceConfigBuilderList()) {

                if (!serviceConfig.hasBindingServiceConfig()) {
                    throw new NotAvailableException("binding service config");
                }

                if (serviceConfig.getBindingServiceConfig().getType().equals(BindingType.OPENHAB)) {
                    String itemId = generateItemName(entry.getMessage(), deviceClassRegistry.getMessage(deviceConfig.getDeviceClassId()).getLabel(), unitConfig.clone().build(), serviceConfig.clone().build(), locationRegistryRemote.getLocationConfigById(entry.getMessage().getPlacementConfig().getLocationId()));

                    boolean itemEntryFound = false;
                    MetaConfig metaConfig = serviceConfig.getBindingServiceConfig().getMetaConfig();
                    for (int i = 0; i < metaConfig.getEntryCount(); i++) {
                        if (metaConfig.getEntry(i).getKey().equals(OPENHAB_BINDING_ITEM_ID)) {
                            itemEntryFound = true;
                            if (!metaConfig.getEntry(i).getValue().equals(itemId)) {
                                metaConfig.toBuilder().setEntry(i, metaConfig.getEntry(i).toBuilder().setValue(itemId).build());
                            }
                        }
                    }
                    if (!itemEntryFound) {
                        metaConfig.toBuilder().addEntry(Entry.newBuilder().setKey(OPENHAB_BINDING_ITEM_ID).setValue(itemId).build());
                    }

                    serviceConfig = serviceConfig.setBindingServiceConfig(serviceConfig.getBindingServiceConfig().toBuilder().setMetaConfig(metaConfig));
                }
                unitConfig.addServiceConfig(serviceConfig);
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig).getMessage(), this);
        }
    }

    public static String generateItemName(final DeviceConfig device, final String deviceClassLabel, final UnitConfig unit, final ServiceConfig service, final LocationConfig location) throws CouldNotPerformException {
        if (device == null) {
            throw new NotAvailableException("deviceconfig");
        }

        if (unit == null) {
            throw new NotAvailableException("unitconfig");
        }

        if (service == null) {
            throw new NotAvailableException("serviceconfig");
        }

        return StringProcessor.transformToIdString(deviceClassLabel)
                + ITEM_SEGMENT_DELIMITER
                + generateLocationId(location)
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(unit.getType().toString())
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformToIdString(unit.getLabel())
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(service.getType().toString());
    }

    public static String generateLocationId(final LocationConfig location) throws CouldNotPerformException {

        if (location == null) {
            throw new NotAvailableException("locationconfig");
        }

        String location_id = "";

        boolean firstEntry = true;
        for (String component : location.getScope().getComponentList()) {
            if (firstEntry) {
                firstEntry = false;
            } else {
                location_id += ITEM_SUBSEGMENT_DELIMITER;
            }
            location_id += component;
        }
        return location_id;
    }

    @Override
    public void reset() {
    }
}
