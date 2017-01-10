package org.openbase.bco.registry.unit.core.consistency.userconfig;

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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserConfigLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig userUnitConfig = entry.getMessage();
        UserConfig userConfig = userUnitConfig.getUserConfig();

        String label = generateUserLabel(userConfig);
        if (!userUnitConfig.hasLabel() || !label.equals(userUnitConfig.getLabel())) {
            throw new EntryModification(entry.setMessage(userUnitConfig.toBuilder().setLabel(label).build()), this);
        }
    }

    private String generateUserLabel(UserConfig userConfig) throws NotAvailableException {
        if (!userConfig.hasFirstName() || userConfig.getFirstName().isEmpty()) {
            throw new NotAvailableException("UserConfig.FirstName");
        }

        if (!userConfig.hasLastName() || userConfig.getLastName().isEmpty()) {
            throw new NotAvailableException("UserConfig.LastName");
        }

        if (!userConfig.hasUserName() || userConfig.getUserName().isEmpty()) {
            throw new NotAvailableException("UserConfig.FirstName");
        }

        return userConfig.getUserName() + " (" + userConfig.getFirstName() + " " + userConfig.getLastName() + ")";
    }
}
