package org.openbase.bco.registry.unit.core.dbconvert.consistency;

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
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractVersionConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.FileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneConfig_1_VersionConsistencyHandler extends AbstractVersionConsistencyHandler<String, SceneConfig, SceneConfig.Builder> {

    private DeviceRegistryRemote deviceRegistry;
    private final Map<String, String> unitConfigIdMap;

    public SceneConfig_1_VersionConsistencyHandler(final DBVersionControl versionControl, final FileSynchronizedRegistry<String, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder>> registry) throws InstantiationException, InterruptedException {
        super(versionControl, registry);
        try {
            this.unitConfigIdMap = new HashMap<>();
            deviceRegistry = new DeviceRegistryRemote();
            deviceRegistry.init();
            deviceRegistry.activate();
            deviceRegistry.waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, SceneConfig, SceneConfig.Builder> entry, ProtoBufMessageMap<String, SceneConfig, SceneConfig.Builder> entryMap, ProtoBufRegistry<String, SceneConfig, SceneConfig.Builder> registry) throws CouldNotPerformException, EntryModification, InitializationException {
        SceneConfig.Builder sceneConfig = entry.getMessage().toBuilder();

        if (unitConfigIdMap.isEmpty()) {
            try {
                for (UnitConfig config : deviceRegistry.getUnitConfigs()) {
                    unitConfigIdMap.put(oldUnitconfigGenerateId(config), config.getId());
                }
            } catch (CouldNotPerformException ex) {
                unitConfigIdMap.clear();
                throw new CouldNotPerformException("Could not build unit id map!", ex);
            }
        }

        boolean modification = false;

        sceneConfig.clearActionConfig();
        for (ActionConfig.Builder actionConfig : entry.getMessage().toBuilder().getActionConfigBuilderList()) {
            if (unitConfigIdMap.containsKey(actionConfig.getUnitId())) {
                actionConfig.setUnitId(unitConfigIdMap.get(actionConfig.getUnitId()));
                modification = true;
            }
            sceneConfig.addActionConfig(actionConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(sceneConfig), this);
        }
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
