package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DalUnitHostIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitDeviceConfigRegistry;

    public DalUnitHostIdConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitDeviceConfigRegistry) {
        this.unitDeviceConfigRegistry = unitDeviceConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig dalUnitConfig = entry.getMessage();

        if (!dalUnitConfig.hasUnitHostId() || dalUnitConfig.getUnitHostId().isEmpty()) {
            throw new VerificationFailedException("DalUnitConfig [" + dalUnitConfig + "] has no unitHostId!");
        } else {
            // TODO: this should be implemented for app hosts as well. Currently there are only devices supported!
            // throws a could not perform exception if the unit host is not registered
            UnitConfig unitHostConfig = unitDeviceConfigRegistry.get(dalUnitConfig.getUnitHostId()).getMessage();
            if (!unitHostConfig.getDeviceConfig().getUnitIdList().contains(dalUnitConfig.getId())) {

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
