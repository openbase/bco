/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import java.util.ArrayList;
import java.util.List;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitGroupMemberListTypesConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitGroupConfig, UnitGroupConfig.Builder> {

    private ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry;
    private ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> unitTemplateRegistry;

    public UnitGroupMemberListTypesConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, DeviceConfig, DeviceConfig.Builder, DeviceRegistry.Builder> deviceConfigRegistry, ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> unitTemplateRegistry) {
        this.deviceConfigRegistry = deviceConfigRegistry;
        this.unitTemplateRegistry = unitTemplateRegistry;
    }
    
    @Override
    public void processData(String id, IdentifiableMessage<String, UnitGroupConfig, UnitGroupConfig.Builder> entry, ProtoBufMessageMapInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> entryMap, ProtoBufRegistryInterface<String, UnitGroupConfig, UnitGroupConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitGroupConfig.Builder unitGroup = entry.getMessage().toBuilder();

        // check if all member units fullfill the according unit type or, if the unit type field is unkown ,have all the according services
        boolean modification = false;
        List<String> memberIds = new ArrayList<>();
        unitGroup.clearMemberId();
        if (unitGroup.getUnitType() == UnitType.UNKNOWN) {
            //check if every unit hast all given services
            for (String memberId : entry.getMessage().getMemberIdList()) {
                UnitConfig unitConfig = getUnitConfigById(memberId);
                boolean skip = false;
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (!unitGroup.getServiceTypeList().contains(serviceConfig.getType())) {
                        skip = true;
                    }
                }
                if (skip) {
                    modification = true;
                    continue;
                }
                memberIds.add(memberId);
            }
        } else {
            for (String memberId : entry.getMessage().getMemberIdList()) {
                UnitType unitType = getUnitConfigById(memberId).getType();
                if (unitType == unitGroup.getUnitType() || getSubTypes(unitGroup.getUnitType()).contains(unitType)) {
                    memberIds.add(memberId);
                } else {
                    modification = true;
                }
            }
        }
        unitGroup.addAllMemberId(memberIds);

        if (modification) {
            throw new EntryModification(entry.setMessage(unitGroup.build()), this);
        }
    }

    private UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> deviceConfig : deviceConfigRegistry.getEntries()) {
            for (UnitConfig unitConfig : deviceConfig.getMessage().getUnitConfigList()) {
                if (unitConfig.getId().equals(unitConfigId)) {
                    return unitConfig;
                }
            }
        }
        throw new NotAvailableException(unitConfigId);
    }
    
    private List<UnitType> getSubTypes(UnitType type) throws CouldNotPerformException {
        List<UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : unitTemplateRegistry.getMessages()) {
            if (template.getIncludedTypeList().contains(type)) {
                unitTypes.add(template.getType());
            }
        }
        return unitTypes;
    }
}
