/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.BindingServiceConfigType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitConfigUnitTemplateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> unitTemplateRegistry;

    List<String> idList = new ArrayList();

    public UnitConfigUnitTemplateConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> unitTemplateRegistry) {
        this.unitTemplateRegistry = unitTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        boolean modification = false;
        for (UnitConfig.Builder unitConfig : deviceConfig.getUnitConfigBuilderList()) {
            UnitTemplate unitTemplate = unitTemplateRegistry.get(unitConfig.getType().toString()).getMessage();
            for (ServiceType serviceType : unitTemplate.getServiceTypeList()) {
                if (!unitConfigContainsServiceType(unitConfig, serviceType)) {
                    unitConfig.addServiceConfig(ServiceConfig.newBuilder().setType(serviceType).setBindingServiceConfig(BindingServiceConfigType.BindingServiceConfig.getDefaultInstance()));
                    modification = true;
                }
            }

            for (int i = 0; i < unitConfig.getServiceConfigCount(); i++) {
                if (!unitTemplate.getServiceTypeList().contains(unitConfig.getServiceConfig(i).getType())) {
                    unitConfig.removeServiceConfig(i);
                    i--;
                    modification = true;
                }
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }
    }

    @Override
    public void reset() {
    }

    private boolean unitConfigContainsServiceType(UnitConfig.Builder unitConfig, ServiceType serviceType) {
        for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            if (serviceConfig.getType() == serviceType) {
                return true;
            }
        }
        return false;
    }
}
