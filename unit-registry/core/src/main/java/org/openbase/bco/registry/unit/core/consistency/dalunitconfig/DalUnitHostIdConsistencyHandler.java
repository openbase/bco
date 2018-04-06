package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DalUnitHostIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitDeviceConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitAppConfigRegistry;

    public DalUnitHostIdConsistencyHandler(
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitDeviceConfigRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitAppConfigRegistry) {
        this.unitDeviceConfigRegistry = unitDeviceConfigRegistry;
        this.unitAppConfigRegistry = unitAppConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig dalUnitConfig = entry.getMessage();

        if (!dalUnitConfig.hasUnitHostId() || dalUnitConfig.getUnitHostId().isEmpty()) {

            // generate host id for virtual units
            if (UnitConfigProcessor.isDalUnit(dalUnitConfig)) {
                throw new EntryModification(entry.setMessage(dalUnitConfig.toBuilder().setUnitHostId(dalUnitConfig.getId())), this);
            }

            throw new VerificationFailedException("DalUnitConfig [" + dalUnitConfig + "] has no unitHostId!");
        } else {
            // skip unit host check if device or app registry is maybe currently registering this unit.
            if (unitDeviceConfigRegistry.isBusyByCurrentThread() || unitAppConfigRegistry.isBusyByCurrentThread()) {
                return;
            }

            // retrieve the config of the unit host
            UnitConfig unitHostConfig;
            if (unitDeviceConfigRegistry.contains(dalUnitConfig.getUnitHostId())) {
                unitHostConfig = unitDeviceConfigRegistry.get(dalUnitConfig.getUnitHostId()).getMessage();
            } else if (unitAppConfigRegistry.contains(dalUnitConfig.getUnitHostId())) {
                unitHostConfig = unitAppConfigRegistry.get(dalUnitConfig.getUnitHostId()).getMessage();
            } else {
                throw new NotAvailableException("Host of DalUnitConfig [" + dalUnitConfig + "] does not exist");
            }

            // retrieve the units the host knows about
            List<String> hostUnitIdList = new ArrayList<>();
            if(unitHostConfig.getType() == UnitType.DEVICE) {
                hostUnitIdList = unitHostConfig.getDeviceConfig().getUnitIdList();
            } else if(unitHostConfig.getType() == UnitType.APP) {
                hostUnitIdList = unitHostConfig.getAppConfig().getUnitIdList();
            }

            // check if the host knows this unit
            if (!hostUnitIdList.contains(dalUnitConfig.getId())) {
                try {
                    if (JPService.getProperty(JPRecoverDB.class).getValue()) {
                        if (!registry.isSandbox()) {
                            logger.warn("Unit[" + dalUnitConfig.getLabel() + "] will be removed because UnitHost[" + unitHostConfig.getLabel() + "] does not know this unit!");
                        }
                        registry.remove(dalUnitConfig);
                        return;
                    }
                } catch (JPNotAvailableException ex) {
                    logger.warn("Could not check JPRecoverDB flag!");
                }

                throw new VerificationFailedException("DalUnitConfig [" + dalUnitConfig.getLabel() + "] is not registered in UnitHost[" + unitHostConfig.getLabel() + "]!");
            }
        }
    }
}
