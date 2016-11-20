package org.openbase.bco.registry.unit.core.consistency.appconfig;

/*
 * #%L
 * REM AppRegistry Core
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
import java.util.Map;
import java.util.TreeMap;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final Registry<String, IdentifiableMessage<String, AppClass, AppClass.Builder>> agentClassRegistry;
    private final Map<String, UnitConfig> appMap;

    public AppScopeConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry, final Registry<String, IdentifiableMessage<String, AppClass, AppClass.Builder>> agentClassRegistry) {
        this.locationRegistry = locationRegistry;
        this.agentClassRegistry = agentClassRegistry;
        this.appMap = new TreeMap<>();
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig appUnitConfig = entry.getMessage();

        if (!appUnitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("agent.placementConfig");
        }

        if (!appUnitConfig.getPlacementConfig().hasLocationId() || appUnitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("app.placementConfig.locationId");
        }
        
        if (!appUnitConfig.getAppConfig().hasAppClassId() || appUnitConfig.getAppConfig().getAppClassId().isEmpty()) {
            throw new NotAvailableException("app.appClassId");
        }

        Scope newScope = ScopeGenerator.generateAppScope(appUnitConfig, agentClassRegistry.get(appUnitConfig.getAppConfig().getAppClassId()).getMessage(), locationRegistry.getMessage(appUnitConfig.getPlacementConfig().getLocationId()));

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(appUnitConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            if (appMap.containsKey(ScopeGenerator.generateStringRep(newScope))) {
                throw new InvalidStateException("Two apps [" + appUnitConfig + "][" + appMap.get(ScopeGenerator.generateStringRep(newScope)) + "] are registered with the same label and location");
            } else {
                appMap.put(ScopeGenerator.generateStringRep(newScope), appUnitConfig);
                entry.setMessage(appUnitConfig.toBuilder().setScope(newScope));
                throw new EntryModification(entry, this);
            }
        }
    }

    @Override
    public void reset() {
        appMap.clear();
    }
}
