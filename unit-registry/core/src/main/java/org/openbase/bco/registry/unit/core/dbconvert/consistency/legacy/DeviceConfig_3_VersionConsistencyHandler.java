package org.openbase.bco.registry.unit.core.dbconvert.consistency.legacy;

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
import java.util.HashMap;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractVersionConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.FileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 * Not supported any more because of rst changes.
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfig_3_VersionConsistencyHandler extends AbstractVersionConsistencyHandler<String, DeviceConfig, DeviceConfig.Builder> {

    private final Map<String, String> unitConfigIdMap;

    public DeviceConfig_3_VersionConsistencyHandler(final DBVersionControl versionControl, final FileSynchronizedRegistry<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>> registry) throws InstantiationException, InterruptedException {
        super(versionControl, registry);
        this.unitConfigIdMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMap<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistry<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
//        DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();
//
//        boolean modification = false;
//
//        deviceConfig.clearUnitConfig();
//        for (UnitConfig.Builder unitConfig : entry.getMessage().toBuilder().getUnitConfigBuilderList()) {
//            if (oldUnitconfigGenerateId(unitConfig.build()).equals(unitConfig.getId())) {
//                String newUnitID = UnitConfigIdGenerator.getInstance().generateId(unitConfig.build());
//                unitConfigIdMap.put(unitConfig.getId(), newUnitID);
//                unitConfig.setId(newUnitID);
//                modification = true;
//            }
//            deviceConfig.addUnitConfig(unitConfig);
//        }
//
//        if (modification) {
//            throw new EntryModification(entry.setMessage(deviceConfig), this);
//        }
    }

    /**
     * This is the old location id generator used for id recovery.
     *
     * @param unitConfig
     * @return
     * @throws CouldNotPerformException
     */
    public String oldUnitconfigGenerateId(UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            if (!unitConfig.hasScope()) {
                throw new NotAvailableException("unitconfig.scope");
            }
            return ScopeGenerator.generateStringRep(unitConfig.getScope());
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate unit id!", ex);
        }
    }
}
