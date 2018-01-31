package org.openbase.bco.registry.unit.core.plugin;

/*-
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
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class UniqueAliasPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private final Map<String, String> aliasUnitIdMap;

    public UniqueAliasPlugin() {
        this.aliasUnitIdMap = new HashMap<>();
    }

    @Override
    public void init(ProtoBufRegistry<String, UnitConfig, Builder> registry) throws InitializationException, InterruptedException {
        super.init(registry);

        try {
            for (UnitConfig unitConfig : registry.getMessages()) {
                for (String alias : unitConfig.getAliasList()) {
                    if (aliasUnitIdMap.containsKey(alias.toLowerCase()) && !aliasUnitIdMap.get(alias.toLowerCase()).equals(unitConfig.getId())) {
                        throw new RejectedException("Alias[" + alias.toLowerCase() + "] of unit[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + ", " + unitConfig.getId() + "] is already used by unit[" + aliasUnitIdMap.get(alias.toLowerCase()) + "]");
                    } else {
                        aliasUnitIdMap.put(alias.toLowerCase(), unitConfig.getId());
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Before a message is registered check if any alias it contains is already used.
     *
     * @param identifiableMessage the message which is going to be registered.
     * @throws RejectedException if another unit already contains one of the aliases of this unit.
     */
    @Override
    public void beforeRegister(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws RejectedException {
        for (String alias : identifiableMessage.getMessage().getAliasList()) {
            if (aliasUnitIdMap.containsKey(alias.toLowerCase())) {
                throw new RejectedException("Alias[" + alias + "] is already used by unit[" + aliasUnitIdMap.get(alias.toLowerCase()) + "]");
            }
        }
    }

    /**
     * Add all aliases of the registered message to the map. This is done after registration because
     * during registration an alias may be added by consistency handler.
     *
     * @param identifiableMessage the message which has been registered.
     */
    @Override
    public void afterRegister(IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        for (String alias : identifiableMessage.getMessage().getAliasList()) {
            aliasUnitIdMap.put(alias.toLowerCase(), identifiableMessage.getMessage().getId());
        }
    }

    /**
     * Before a message is updated check if any alias it contains is already used by another unit.
     * If not it will be added to the current map of aliases. Additionally all aliases which have been
     * removed from the unit will also be removed of the internal map.
     *
     * @param identifiableMessage the message which is going to be updated.
     * @throws RejectedException if another unit already contains one of the new aliases of this unit.
     */
    @Override
    public void beforeUpdate(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws RejectedException {
        // check for all aliases of the message if they are new or already taken
        List<String> aliasesAdded = new ArrayList<>();
        try {
            for (String alias : identifiableMessage.getMessage().getAliasList()) {
                if (aliasUnitIdMap.containsKey(alias.toLowerCase()) && !aliasUnitIdMap.get(alias.toLowerCase()).equals(identifiableMessage.getMessage().getId())) {
                    throw new RejectedException("Alias[" + alias.toLowerCase() + "] is already used by unit[" + aliasUnitIdMap.get(alias.toLowerCase()) + "]");
                } else {
                    aliasesAdded.add(alias.toLowerCase());
                    aliasUnitIdMap.put(alias.toLowerCase(), identifiableMessage.getMessage().getId());
                }
            }
        } catch (RejectedException ex) {
            // rollback if exception is thrown
            for (String alias : aliasesAdded) {
                aliasUnitIdMap.remove(alias.toLowerCase());
            }
            throw ex;
        }

        // iterate over map and save all aliases to be removed
        List<String> aliasesToRemove = new ArrayList<>();
        for (Entry<String, String> entry : aliasUnitIdMap.entrySet()) {
            for(String alias : identifiableMessage.getMessage().getAliasList()) {
                if(alias.toLowerCase().equals(entry.getKey())) {
                    aliasesToRemove.add(entry.getKey());
                }
            }
        }

        // remove the aliases from the map
        for (String alias : aliasesToRemove) {
            aliasUnitIdMap.remove(alias);
        }
    }

    /**
     * Remote all aliases from a message which has been removed.
     *
     * @param identifiableMessage the message which has been removed.
     */
    @Override
    public void afterRemove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        for (String alias : identifiableMessage.getMessage().getAliasList()) {
            aliasUnitIdMap.remove(alias.toLowerCase());
        }
    }
}
