package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
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
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AliasMapUpdatePlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private final Map<String, String> aliasIdMap;
    private final SyncObject aliasIdMapLock;

    private List<String> temporaryAliasList;

    public AliasMapUpdatePlugin(final Map<String, String> aliasIdMap, final SyncObject aliasIdMapLock) {
        this.aliasIdMap = aliasIdMap;
        this.aliasIdMapLock = aliasIdMapLock;
        this.temporaryAliasList = new ArrayList<>();
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        final UnitConfig unitConfig = identifiableMessage.getMessage();

        // add all aliases of the registered unit config
        synchronized (aliasIdMapLock) {
            unitConfig.getAliasList().forEach(alias -> aliasIdMap.put(alias.toLowerCase(), unitConfig.getId()));
        }
    }

    @Override
    public void beforeRemove(IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        final UnitConfig unitConfig = identifiableMessage.getMessage();

        // remove all aliases of the removed unit config
        synchronized (aliasIdMapLock) {
            unitConfig.getAliasList().forEach(alias -> aliasIdMap.remove(alias.toLowerCase()));
        }
    }

    @Override
    public void beforeUpdate(IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws RejectedException {
        // save all aliases by the unit config before it is updated
        try {
            temporaryAliasList = new ArrayList<>(getRegistry().getMessage(identifiableMessage.getId()).getAliasList());
        } catch (CouldNotPerformException ex) {
            throw new RejectedException("Could not get message which is updated", ex);
        }
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        final UnitConfig unitConfig = identifiableMessage.getMessage();

        synchronized (aliasIdMapLock) {
            // add all new aliases
            unitConfig.getAliasList().forEach(alias -> {
                if (!temporaryAliasList.contains(alias)) {
                    aliasIdMap.put(alias.toLowerCase(), unitConfig.getId());
                }
            });

            // remove all removed aliases
            temporaryAliasList.forEach(alias -> {
                if (!unitConfig.getAliasList().contains(alias)) {
                    aliasIdMap.remove(alias.toLowerCase());
                }
            });
        }

        temporaryAliasList.clear();
    }

}
