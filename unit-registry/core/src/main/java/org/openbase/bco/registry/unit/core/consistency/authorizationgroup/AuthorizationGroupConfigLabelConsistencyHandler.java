package org.openbase.bco.registry.unit.core.consistency.authorizationgroup;

/*
 * #%L
 * BCO Registry Unit Core
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
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.configuration.LabelType.Label;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AuthorizationGroupConfigLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Map<String, String> authorizationGroupMap;

    public AuthorizationGroupConfigLabelConsistencyHandler() {
        this.authorizationGroupMap = new HashMap<>();
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder authorizationGroupUnitConfig = entry.getMessage().toBuilder();

        if (!authorizationGroupUnitConfig.hasLabel()) {
            if (authorizationGroupUnitConfig.getAliasCount() <= 1) {
                throw new InvalidStateException("Alias not provided by Unit[" + authorizationGroupUnitConfig.getId() + "]!");
            }
            LabelProcessor.addLabel(authorizationGroupUnitConfig.getLabelBuilder(), Locale.ENGLISH, authorizationGroupUnitConfig.getAlias(0));
            throw new EntryModification(entry.setMessage(authorizationGroupUnitConfig), this);
        }

        for (Label.MapFieldEntry labelMapEntry : authorizationGroupUnitConfig.getLabel().getEntryList()) {
            for (String value : labelMapEntry.getValueList()) {
                if (!authorizationGroupMap.containsKey(value)) {
                    authorizationGroupMap.put(value, authorizationGroupUnitConfig.getAlias(0));
                } else {
                    throw new InvalidStateException("AuthorizationGroup [" + authorizationGroupUnitConfig.getAlias(0) + "] and authorizationGroup [" + authorizationGroupMap.get(value) + "] are registered with the same label!");
                }
            }
        }
    }

    @Override
    public void reset() {
        authorizationGroupMap.clear();
    }
}
