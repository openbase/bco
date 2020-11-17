package org.openbase.bco.registry.clazz.core.consistency;

/*-
 * #%L
 * BCO Registry Class Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;

import java.util.ArrayList;

/**
 * Make sure that all nested gateway ids exist and remove them if they don't.
 */
public class GatewayClassNestedGatewayConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, GatewayClass, GatewayClass.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, GatewayClass, GatewayClass.Builder> entry, ProtoBufMessageMap<String, GatewayClass, GatewayClass.Builder> entryMap, ProtoBufRegistry<String, GatewayClass, GatewayClass.Builder> registry) throws CouldNotPerformException, EntryModification {
        final GatewayClass.Builder builder = entry.getMessage().toBuilder();

        final ArrayList<String> nestedGatewayIdList = new ArrayList<>(builder.getNestedGatewayIdList());
        builder.clearNestedGatewayId();
        for (final String nestedGatewayId : nestedGatewayIdList) {
            try {
                registry.getMessage(nestedGatewayId);
                builder.addNestedGatewayId(nestedGatewayId);
            } catch (NotAvailableException ex) {
                // do not re-add id
            }
        }

        if (builder.getNestedGatewayIdList().size() != nestedGatewayIdList.size()) {
            throw new EntryModification(entry.setMessage(builder, this), this);
        }
    }
}
