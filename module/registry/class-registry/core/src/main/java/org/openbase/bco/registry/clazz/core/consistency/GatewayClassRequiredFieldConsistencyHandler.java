package org.openbase.bco.registry.clazz.core.consistency;

/*-
 * #%L
 * BCO Registry Class Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass.Builder;

/**
 * Consistency handler that makes sure that every gateway class has a company and a label.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class GatewayClassRequiredFieldConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, GatewayClass,  Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, GatewayClass, Builder> entry, ProtoBufMessageMap<String, GatewayClass, Builder> entryMap, ProtoBufRegistry<String, GatewayClass, Builder> registry) throws CouldNotPerformException {
        if (!entry.getMessage().hasLabel()) {
            try {
                LabelProcessor.getBestMatch(entry.getMessage().getLabel());
            } catch (NotAvailableException ex) {
                throw new InvalidStateException("GatewayClass is missing label");
            }
        }
    }
}
