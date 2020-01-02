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

import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This plugin will create a default BCO and admin authorization group.
 * Additionally it makes sure that these groups always contain their destined aliases and that
 * they will not be removed.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorizationGroupCreationPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private static final String ADMIN_GROUP_LABEL = "Admin";
    private static final String BCO_GROUP_LABEL = "BCO";

    private final Map<String, String> aliasLabelMap;
    private final Map<String, String> idAliasMap;

    //TODO: these groups should not be found by label but by alias
    public AuthorizationGroupCreationPlugin() {
        this.aliasLabelMap = new HashMap<>();
        this.aliasLabelMap.put(UnitRegistry.ADMIN_GROUP_ALIAS, ADMIN_GROUP_LABEL);
        this.aliasLabelMap.put(UnitRegistry.BCO_GROUP_ALIAS, BCO_GROUP_LABEL);

        this.idAliasMap = new HashMap<>();
    }

    @Override
    public void init(final ProtoBufRegistry<String, UnitConfig, Builder> authorizationGroupRegistry) throws InitializationException, InterruptedException {
        super.init(authorizationGroupRegistry);
        try {
            // create missing authorization groups
            for (final Entry<String, String> entry : aliasLabelMap.entrySet()) {
                String id;
                try {
                    id = getAuthorizationGroupByAlias(entry.getKey()).getId();
                } catch (NotAvailableException ex) {
                    // not available so register;
                    final UnitConfig.Builder authorizationGroup = UnitConfig.newBuilder();
                    authorizationGroup.addAlias(entry.getKey()).setUnitType(UnitType.AUTHORIZATION_GROUP);
                    LabelProcessor.addLabel(authorizationGroup.getLabelBuilder(), Locale.ENGLISH, entry.getValue());
                    id = authorizationGroupRegistry.register(authorizationGroup.build()).getId();
                }
                idAliasMap.put(id, entry.getKey());
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private UnitConfig getAuthorizationGroupByAlias(final String alias) throws CouldNotPerformException {
        for (final UnitConfig unitConfig : getRegistry().getMessages()) {
            for (final String unitAlias : unitConfig.getAliasList()) {
                if (unitAlias.equals(alias)) {
                    return unitConfig;
                }
            }
        }
        throw new NotAvailableException("AuthorizationGroup with alias[" + alias + "]");
    }

    @Override
    public void beforeUpdate(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws RejectedException {
        final UnitConfig authorizationGroup = identifiableMessage.getMessage();
        if (!idAliasMap.containsKey(authorizationGroup.getId())) {
            return;
        }

        final String expectedAlias = idAliasMap.get(authorizationGroup.getId());
        for (final String alias : authorizationGroup.getAliasList()) {
            if (alias.equals(expectedAlias)) {
                return;
            }
        }

        throw new RejectedException("AuthorizationGroup[" + authorizationGroup.getId() + "] should contain the alias[" + expectedAlias + "]");
    }

    @Override
    public void beforeRemove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws RejectedException {
        final String id = identifiableMessage.getMessage().getId();
        if (idAliasMap.containsKey(id)) {
            throw new RejectedException("AuthorizationGroup[" + identifiableMessage.getMessage().getId() + "] cannot be removed because it is the " + idAliasMap.get(id));
        }
    }
}
