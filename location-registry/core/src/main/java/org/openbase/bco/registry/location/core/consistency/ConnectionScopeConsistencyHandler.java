package org.openbase.bco.registry.location.core.consistency;

/*
 * #%L
 * REM LocationRegistry Core
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistryInterface;
import rst.rsb.ScopeType.Scope;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionScopeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ConnectionConfig, ConnectionConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRegistry;

    public ConnectionScopeConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRegistry) {
        this.locationConfigRegistry = locationConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry, ProtoBufMessageMapInterface<String, ConnectionConfig, ConnectionConfig.Builder> entryMap, ProtoBufRegistryInterface<String, ConnectionConfig, ConnectionConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        ConnectionConfig connectionConfig = entry.getMessage();

        Scope newScope = ScopeGenerator.generateConnectionScope(connectionConfig, locationConfigRegistry.getMessage(connectionConfig.getPlacementConfig().getLocationId()));
        // verify and update scope
        if (!ScopeGenerator.generateStringRep(connectionConfig.getScope()).equals(ScopeGenerator.generateStringRep(newScope))) {
            entry.setMessage(connectionConfig.toBuilder().setScope(newScope));
            throw new EntryModification(entry, this);
        }
    }
}
