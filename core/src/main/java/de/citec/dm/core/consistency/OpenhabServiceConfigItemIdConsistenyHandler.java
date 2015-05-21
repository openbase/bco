/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.rsb.container.IdentifiableMessage;
import de.citec.jul.extension.rsb.container.ProtoBufMessageMapInterface;
import de.citec.jul.processing.StringProcessor;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.binding.BindingTypeHolderType.BindingTypeHolder.BindingType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.OpenHABBindingServiceConfigType.OpenHABBindingServiceConfig;
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

    public OpenhabServiceConfigItemIdConsistenyHandler() {
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
                if (serviceConfig.getBindingServiceConfig().getType().equals(BindingType.OPENHAB)) {
                    
                    OpenHABBindingServiceConfig.Builder openHABServiceConfig;
                    if(!serviceConfig.getBindingServiceConfig().hasOpenhabBindingServiceConfig()) {
                        openHABServiceConfig = OpenHABBindingServiceConfig.newBuilder();
                    } else {
                        openHABServiceConfig = serviceConfig.getBindingServiceConfig().getOpenhabBindingServiceConfig().toBuilder();
                    }
                     
                    if (!openHABServiceConfig.hasItemId() || openHABServiceConfig.getItemId().isEmpty() || !openHABServiceConfig.getItemId().equals(generateItemName(entry.getMessage(), unitConfig.clone().build(), serviceConfig.clone().build()))) {
                        openHABServiceConfig.setItemId(generateItemName(entry.getMessage(), unitConfig.clone().build(), serviceConfig.clone().build()));
                        modification = true;
                    }
                    serviceConfig = serviceConfig.setBindingServiceConfig(serviceConfig.getBindingServiceConfig().toBuilder().setOpenhabBindingServiceConfig(openHABServiceConfig));
                }
                unitConfig.addServiceConfig(serviceConfig);
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig).getMessage(), this);
        }
    }

    public static String generateItemName(final DeviceConfig device, final UnitConfig unit, final ServiceConfig service) throws CouldNotPerformException {
        if(device == null) {
            throw new NotAvailableException("deviceconfig");
        }
        
        if(unit == null) {
            throw new NotAvailableException("unitconfig");
        }
        
        if(service == null) {
            throw new NotAvailableException("serviceconfig");
        }
        
        return device.getDeviceClass().getLabel()
                + ITEM_SEGMENT_DELIMITER
                + generateLocationId(unit.getPlacementConfig().getLocationConfig())
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(unit.getTemplate().getType().toString())
                + ITEM_SEGMENT_DELIMITER
                + unit.getLabel()
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
}
