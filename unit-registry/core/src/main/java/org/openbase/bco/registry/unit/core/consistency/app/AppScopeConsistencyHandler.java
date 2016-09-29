package org.openbase.bco.registry.unit.core.consistency.app;

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
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final Map<String, UnitConfig> appMap;

    public AppScopeConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
        this.appMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig app = entry.getMessage();

        if (!app.hasPlacementConfig()) {
            throw new NotAvailableException("agent.placementConfig");
        }

        if (!app.getPlacementConfig().hasLocationId() || app.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("app.placementConfig.locationId");
        }

        Scope newScope = ScopeGenerator.generateAppScope(app, locationRegistry.getMessage(app.getPlacementConfig().getLocationId()));

        // verify and update scope
        if (!ScopeGenerator.generateStringRep(app.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            if (appMap.containsKey(ScopeGenerator.generateStringRep(newScope))) {
                throw new InvalidStateException("Two apps [" + app + "][" + appMap.get(ScopeGenerator.generateStringRep(newScope)) + "] are registered with the same label and location");
            } else {
                appMap.put(ScopeGenerator.generateStringRep(newScope), app);
                entry.setMessage(app.toBuilder().setScope(newScope));
                throw new EntryModification(entry, this);
            }
        }
    }

    @Override
    public void reset() {
        appMap.clear();
    }
}
