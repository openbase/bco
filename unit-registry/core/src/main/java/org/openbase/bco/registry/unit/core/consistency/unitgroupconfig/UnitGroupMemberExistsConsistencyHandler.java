package org.openbase.bco.registry.unit.core.consistency.unitgroupconfig;

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
import java.util.ArrayList;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupMemberExistsConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> agentRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> sceneRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitGroupRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userRegistry;

    public UnitGroupMemberExistsConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> agentRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> connectionRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> scneRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitGroupRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userRegistry) {
        this.agentRegistry = agentRegistry;
        this.appRegistry = appRegistry;
        this.authorizationGroupRegistry = authorizationGroupRegistry;
        this.connectionRegistry = connectionRegistry;
        this.dalUnitRegistry = dalUnitRegistry;
        this.deviceRegistry = deviceRegistry;
        this.locationRegistry = locationRegistry;
        this.sceneRegistry = scneRegistry;
        this.unitGroupRegistry = unitGroupRegistry;
        this.userRegistry = userRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitGroupUnitConfig = entry.getMessage().toBuilder();
        UnitGroupConfig.Builder unitGroup = unitGroupUnitConfig.getUnitGroupConfigBuilder();

        // remove a member id if the according unit does not exists or it is the group id itself
        boolean modification = false;
        List<String> unitConfigsIds = new ArrayList<>();
        unitGroup.clearMemberId();
        for (String unitConfigId : entry.getMessage().getUnitGroupConfig().getMemberIdList()) {
            if (!unitConfigId.equals(unitGroupUnitConfig.getId()) && containsUnitConfigById(unitConfigId)) {
                unitConfigsIds.add(unitConfigId);
            } else {
                modification = true;
            }
        }
        unitGroup.addAllMemberId(unitConfigsIds);

        if (modification) {
            throw new EntryModification(entry.setMessage(unitGroupUnitConfig), this);
        }
    }

    public boolean containsUnitConfigById(String unitConfigId) throws CouldNotPerformException {
        return agentRegistry.contains(unitConfigId)
                || appRegistry.contains(unitConfigId)
                || authorizationGroupRegistry.contains(unitConfigId)
                || connectionRegistry.contains(unitConfigId)
                || dalUnitRegistry.contains(unitConfigId)
                || deviceRegistry.contains(unitConfigId)
                || locationRegistry.contains(unitConfigId)
                || sceneRegistry.contains(unitConfigId)
                || unitGroupRegistry.contains(unitConfigId)
                || userRegistry.contains(unitConfigId);
    }
}
