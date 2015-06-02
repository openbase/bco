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
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.binding.BindingTypeHolderType.BindingTypeHolder.BindingType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.BindingServiceConfigType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author thuxohl
 */
public class ServiceConfigBindingTypeConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

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
                    throw new NotAvailableException("serviceconfig.bindingserviceconfig");
                }

                BindingServiceConfigType.BindingServiceConfig bindingServiceConfig = serviceConfig.getBindingServiceConfig();

                if (!deviceConfig.hasDeviceClass()) {
                    throw new NotAvailableException("deviceclass");
                }

                if (!deviceConfig.getDeviceClass().hasBindingConfig()) {
                    throw new NotAvailableException("deviceclass.bindingconfig");
                }

                if (!deviceConfig.getDeviceClass().getBindingConfig().hasType() || deviceConfig.getDeviceClass().getBindingConfig().getType() == BindingType.UNKNOWN) {
                    throw new NotAvailableException("deviceclass.bindingconfig.type");
                }

                BindingType bindingType = deviceConfig.getDeviceClass().getBindingConfig().getType();

                if (!bindingServiceConfig.hasType() || bindingServiceConfig.getType() != bindingType) {
                    serviceConfig.setBindingServiceConfig(bindingServiceConfig.toBuilder().setType(bindingType).build());
                    modification = true;
                }
                unitConfig.addServiceConfig(serviceConfig);
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig).getMessage(), this);
        }
    }

    @Override
    public void reset() {
    }
}
