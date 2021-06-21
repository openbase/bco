package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupMemberListTypesConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> agentRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> sceneRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitGroupRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitRegistry;

    public UnitGroupMemberListTypesConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> agentRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> sceneRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitGroupRegistry,
                                                      final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitRegistry) {
        this.agentRegistry = agentRegistry;
        this.appRegistry = appRegistry;
        this.authorizationGroupRegistry = authorizationGroupRegistry;
        this.connectionRegistry = connectionRegistry;
        this.dalUnitRegistry = dalUnitRegistry;
        this.deviceRegistry = deviceRegistry;
        this.locationRegistry = locationRegistry;
        this.sceneRegistry = sceneRegistry;
        this.unitGroupRegistry = unitGroupRegistry;
        this.unitRegistry = unitRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitGroupUnitConfig = entry.getMessage().toBuilder();
        UnitGroupConfig.Builder unitGroup = unitGroupUnitConfig.getUnitGroupConfigBuilder();

        // check if all member units fulfill the according unit type.
        boolean modification = false;
        if (unitGroup.getUnitType() != UnitType.UNKNOWN) {
            final List<String> memberIds = new ArrayList<>();
            unitGroup.clearMemberId();
            for (String memberId : entry.getMessage().getUnitGroupConfig().getMemberIdList()) {
                UnitType unitType = getUnitConfigById(memberId).getUnitType();
                if (unitType == unitGroup.getUnitType() || getSubTypes(unitGroup.getUnitType()).contains(unitType)) {
                    memberIds.add(memberId);
                } else {
                    modification = true;
                }
            }
            unitGroup.addAllMemberId(memberIds);
        }

        // compute service list
        final HashMap<String, ServiceConfig> serviceConfigMap = new HashMap<>();
        for (String memberId : unitGroup.getMemberIdList()) {
            serviceLoop: for (ServiceConfig memberServiceConfig : getUnitConfigById(memberId).getServiceConfigList()) {
                boolean supported = true;
                for (String memberToCompare : entry.getMessage().getUnitGroupConfig().getMemberIdList()) {
                    boolean match = false;

                    // check if member supports the service
                    for (ServiceConfig memberToCompareServiceConfig : getUnitConfigById(memberToCompare).getServiceConfigList()) {
                        if (memberServiceConfig.getServiceDescription().getServiceType() == memberToCompareServiceConfig.getServiceDescription().getServiceType() &&
                                memberServiceConfig.getServiceDescription().getPattern() == memberToCompareServiceConfig.getServiceDescription().getPattern()) {
                            match = true;
                            break;
                        }
                    }

                    // if no match found that skip this service
                    if(!match) {
                        continue serviceLoop;
                    }
                }

                // store service config if supported but make sure no service type service pattern pair is registered twice.
                serviceConfigMap.put(memberServiceConfig.getServiceDescription().getServiceType().name() + memberServiceConfig.getServiceDescription().getPattern(), memberServiceConfig);
            }

            final List<ServiceDescription> originalServices = unitGroup.getServiceDescriptionList();
            unitGroup.clearServiceDescription();
            for (ServiceConfig value : serviceConfigMap.values()) {
                unitGroup.addServiceDescription(
                        ServiceDescription.newBuilder()
                                .setServiceType(value.getServiceDescription().getServiceType())
                                .setPattern(value.getServiceDescription().getPattern())
                                .setAggregated(true)
                                .setServiceTemplateId(value.getServiceDescription().getServiceTemplateId()).build());
            }
            if (!unitGroup.getServiceDescriptionList().equals(originalServices)) {
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitGroupUnitConfig, this), this);
        }
    }

    private UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        if (dalUnitRegistry.contains(unitConfigId)) {
            return dalUnitRegistry.getMessage(unitConfigId);
        } else if (agentRegistry.contains(unitConfigId)) {
            return agentRegistry.getMessage(unitConfigId);
        } else if (appRegistry.contains(unitConfigId)) {
            return appRegistry.getMessage(unitConfigId);
        } else if (authorizationGroupRegistry.contains(unitConfigId)) {
            return authorizationGroupRegistry.getMessage(unitConfigId);
        } else if (connectionRegistry.contains(unitConfigId)) {
            return connectionRegistry.getMessage(unitConfigId);
        } else if (deviceRegistry.contains(unitConfigId)) {
            return deviceRegistry.getMessage(unitConfigId);
        } else if (locationRegistry.contains(unitConfigId)) {
            return locationRegistry.getMessage(unitConfigId);
        } else if (sceneRegistry.contains(unitConfigId)) {
            return sceneRegistry.getMessage(unitConfigId);
        } else if (unitGroupRegistry.contains(unitConfigId)) {
            return unitGroupRegistry.getMessage(unitConfigId);
        } else if (unitRegistry.contains(unitConfigId)) {
            return unitRegistry.getMessage(unitConfigId);
        }
        throw new NotAvailableException(unitConfigId);
    }

    private List<UnitType> getSubTypes(UnitType type) throws CouldNotPerformException {
        List<UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : CachedTemplateRegistryRemote.getRegistry().getUnitTemplates()) {
            if (template.getSuperTypeList().contains(type)) {
                unitTypes.add(template.getUnitType());
            }
        }
        return unitTypes;
    }
}
