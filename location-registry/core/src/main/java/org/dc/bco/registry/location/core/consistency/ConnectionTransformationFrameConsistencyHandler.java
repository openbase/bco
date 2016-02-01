/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core.consistency;

/*
 * #%L
 * REM LocationRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import de.citec.jul.extension.rst.storage.registry.consistency.AbstractTransformationFrameConsistencyHandler;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author mpohling
 */
public class ConnectionTransformationFrameConsistencyHandler extends AbstractTransformationFrameConsistencyHandler<String, ConnectionConfig, ConnectionConfig.Builder> {

    public ConnectionTransformationFrameConsistencyHandler(final ProtoBufRegistryInterface<String, LocationConfigType.LocationConfig, LocationConfigType.LocationConfig.Builder> locationRegistry) {
        super(locationRegistry);
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry, final ProtoBufMessageMapInterface<String, ConnectionConfig, ConnectionConfig.Builder> entryMap, final ProtoBufRegistryInterface<String, ConnectionConfig, ConnectionConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        ConnectionConfig connectionConfig = entry.getMessage();
        PlacementConfig placementConfig = verifyAndUpdatePlacement(connectionConfig.getLabel(), connectionConfig.getPlacementConfig());

        if(placementConfig != null) {
            entry.setMessage(connectionConfig.toBuilder().setPlacementConfig(placementConfig));
            throw new EntryModification(entry, this);
        }
    }
}
