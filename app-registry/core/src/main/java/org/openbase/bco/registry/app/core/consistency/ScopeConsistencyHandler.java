package org.openbase.bco.registry.app.core.consistency;

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
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.rsb.ScopeType.Scope;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, AppConfig, AppConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;
    private final Map<String, AppConfig> appMap;

    public ScopeConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
        this.appMap = new TreeMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, AppConfig, AppConfig.Builder> entry, ProtoBufMessageMap<String, AppConfig, AppConfig.Builder> entryMap, ProtoBufRegistry<String, AppConfig, AppConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AppConfig appConfig = entry.getMessage();

        if (!appConfig.hasLocationId()) {
            throw new NotAvailableException("appConfig.locationId");
        }
        if (appConfig.getLocationId().isEmpty()) {
            throw new NotAvailableException("Field appConfig.locationId is empty");
        }

        Scope newScope = ScopeGenerator.generateAppScope(appConfig, locationRegistryRemote.getLocationConfigById(appConfig.getLocationId()));
//
        // verify and update scope
        if (!ScopeGenerator.generateStringRep(appConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            if (appMap.containsKey(ScopeGenerator.generateStringRep(newScope))) {
                throw new InvalidStateException("Two apps [" + appConfig + "][" + appMap.get(ScopeGenerator.generateStringRep(newScope)) + "] registered with the same label and location");
            } else {
                appMap.put(ScopeGenerator.generateStringRep(newScope), appConfig);
                entry.setMessage(appConfig.toBuilder().setScope(newScope));
                throw new EntryModification(entry, this);
            }
        }
    }

    @Override
    public void reset() {
        appMap.clear();
    }
}
