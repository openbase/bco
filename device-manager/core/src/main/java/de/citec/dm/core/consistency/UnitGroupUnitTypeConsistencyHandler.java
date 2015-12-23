/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.List;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitGroupUnitTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitGroupConfig, UnitGroupConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistryType.DeviceRegistry.Builder> unitTemplateRegistry;

    public UnitGroupUnitTypeConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistryType.DeviceRegistry.Builder> unitTemplateRegistry) {
        this.unitTemplateRegistry = unitTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> entry, ProtoBufMessageMapInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitGroupConfig.Builder unitGroup = entry.getMessage().toBuilder();
        if (unitGroup.hasUnitType() && !(unitGroup.getUnitType() == UnitType.UNKNOWN)) {
            if (unitGroup.getServiceTypeList().isEmpty()) {
                throw new EntryModification(entry.setMessage(unitGroup.addAllServiceType(unitTemplateRegistry.get(unitGroup.getUnitType().toString()).getMessage().getServiceTypeList())), this);
            }
            if (!unitTemplateHasSameServices(unitGroup.getUnitType(), unitGroup.getServiceTypeList())) {
                throw new EntryModification(entry.setMessage(unitGroup.setUnitType(UnitType.UNKNOWN)), this);
            }
        }
    }

    private boolean unitTemplateHasSameServices(UnitType unitType, List<ServiceType> serviceTypes) throws CouldNotPerformException {
        UnitTemplate unitTemplate = unitTemplateRegistry.get(unitType.toString()).getMessage();
        if (!serviceTypes.stream().noneMatch((serviceType) -> (!unitTemplate.getServiceTypeList().contains(serviceType)))) {
            return false;
        }
        return unitTemplate.getServiceTypeList().stream().noneMatch((serviceType) -> (!serviceTypes.contains(serviceType)));
    }

}
