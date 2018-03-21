package org.openbase.bco.registry.device.core.consistency;

/*-
 * #%L
 * BCO Registry Device Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceClassType.DeviceClass.Builder;

/**
 * Consistency handler that makes sure that every device class has a company and a label.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceClassRequiredFieldConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceClass, Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceClass, Builder> entry, ProtoBufMessageMap<String, DeviceClass, Builder> entryMap, ProtoBufRegistry<String, DeviceClass, Builder> registry) throws CouldNotPerformException {
        if (!entry.getMessage().hasCompany() || entry.getMessage().getCompany().isEmpty()) {
            throw new InvalidStateException("DeviceClass is missing company");
        }

        if (!entry.getMessage().hasLabel() || entry.getMessage().getLabel().isEmpty()) {
            throw new InvalidStateException("DeviceClass is missing label");
        }
    }
}
