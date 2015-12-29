/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.binding.BindingTypeHolderType.BindingTypeHolder.BindingType;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.service.BindingServiceConfigType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author thuxohl
 */
public class ServiceConfigBindingTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry;

    public ServiceConfigBindingTypeConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceClassType.DeviceClass, DeviceClassType.DeviceClass.Builder, DeviceRegistryType.DeviceRegistry.Builder> deviceClassRegistry) {
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
                    throw new NotAvailableException("serviceconfig.bindingserviceconfig");
                }

                BindingServiceConfigType.BindingServiceConfig bindingServiceConfig = serviceConfig.getBindingServiceConfig();

                if (!deviceConfig.hasDeviceClassId()) {
                    throw new NotAvailableException("deviceclass");
                }

                DeviceClassType.DeviceClass deviceClass = deviceClassRegistry.getMessage(deviceConfig.getDeviceClassId());
                if (!deviceClass.hasBindingConfig()) {
                    throw new NotAvailableException("deviceclass.bindingconfig");
                }

                if (!deviceClass.getBindingConfig().hasType() || deviceClass.getBindingConfig().getType() == BindingType.UNKNOWN) {
                    throw new NotAvailableException("deviceclass.bindingconfig.type");
                }

                BindingType bindingType = deviceClass.getBindingConfig().getType();

                if (!bindingServiceConfig.hasType() || bindingServiceConfig.getType() != bindingType) {
                    serviceConfig.setBindingServiceConfig(bindingServiceConfig.toBuilder().setType(bindingType).build());
                    modification = true;
                }
                unitConfig.addServiceConfig(serviceConfig);
            }
            deviceConfig.addUnitConfig(unitConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    @Override
    public void reset() {
    }
}
