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

import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;

import java.util.concurrent.ExecutionException;

/**
 * This plugin creates a user for every agent and app.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitUserCreationPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    public static final String UNIT_ID_KEY = "UNIT_ID";

    private final ProtoBufRegistry<String, UnitConfig, Builder> userRegistry;
    private final ProtoBufRegistry<String, UnitConfig, Builder> locationRegistry;

    public UnitUserCreationPlugin(final ProtoBufRegistry<String, UnitConfig, Builder> userRegistry,
                                  final ProtoBufRegistry<String, UnitConfig, Builder> locationRegistry) {
        this.userRegistry = userRegistry;
        this.locationRegistry = locationRegistry;
    }

    @Override
    public void init(ProtoBufRegistry<String, UnitConfig, Builder> registry) throws InitializationException, InterruptedException {
        super.init(registry);

        try {
            for (final UnitConfig unitConfig : registry.getMessages()) {
                try {
                    final UnitConfig userUnitConfig = findUser(unitConfig);
                    if (!CachedAuthenticationRemote.getRemote().hasUser(userUnitConfig.getId()).get()) {
                        registerUserAtAuthenticator(userUnitConfig.getId());
                    }
                } catch (NotAvailableException ex) {
                    registerUser(unitConfig);
                }
            }
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void beforeRemove(IdentifiableMessage<String, UnitConfig, Builder> unitConfig) throws RejectedException {
        try {
            userRegistry.remove(findUser(unitConfig.getMessage()));
            //TODO: also remove user from authenticator... currently only possible as admin
        } catch (CouldNotPerformException e) {
            throw new RejectedException("Could not remove user of unit[" + unitConfig.getMessage().getAlias(0) + "]");
        }
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, UnitConfig, Builder> unitConfig) throws CouldNotPerformException {
        registerUser(unitConfig.getMessage());
    }

    private UnitConfig findUser(final UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            return findUser(unitConfig.getId(), userRegistry);
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("User for unit[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "]");
        }
    }

    public static UnitConfig findUser(final String unitId, final ProtoBufRegistry<String, UnitConfig, Builder> userRegistry) throws CouldNotPerformException {
        for (final UnitConfig userUnitConfig : userRegistry.getMessages()) {
            // skip users that are not system user
            if (!userUnitConfig.getUserConfig().getSystemUser()) {
                continue;
            }
            MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider(userUnitConfig.getUserConfig().getUserName() + MetaConfig.class.getSimpleName(), userUnitConfig.getMetaConfig()));
            try {
                final String id = metaConfigPool.getValue(UNIT_ID_KEY);
                if (id.equals(unitId)) {
                    return userUnitConfig;
                }
            } catch (NotAvailableException ex) {
                // ignore user units which are not available
            }
        }
        throw new NotAvailableException("User for unit[" + unitId + "]");
    }

    private String getUsername(final UnitConfig unitConfig) throws CouldNotPerformException {
        String username = LabelProcessor.getBestMatch(unitConfig.getLabel());
        username += "@";
        username += LabelProcessor.getBestMatch(locationRegistry.getMessage(unitConfig.getPlacementConfig().getLocationId()).getLabel());
        return username;
    }

    private void registerUser(final UnitConfig unitConfig) throws CouldNotPerformException {
        final UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder();
        userUnitConfig.setUnitType(UnitType.USER);
        final Entry.Builder entry = userUnitConfig.getMetaConfigBuilder().addEntryBuilder();
        entry.setKey(UNIT_ID_KEY).setValue(unitConfig.getId());
        final UserConfig.Builder userConfig = userUnitConfig.getUserConfigBuilder();
        userConfig.setSystemUser(true);
        userConfig.setUserName(getUsername(unitConfig));
        registerUserAtAuthenticator(userRegistry.register(userUnitConfig.build()).getId());
    }

    private void registerUserAtAuthenticator(final String id) throws CouldNotPerformException {
        if (!SessionManager.getInstance().isLoggedIn()) {
            String bcoUserId = "";
            for (UnitConfig message : userRegistry.getMessages()) {
                if (message.getAliasList().contains(UnitRegistry.BCO_USER_ALIAS)) {
                    bcoUserId = message.getId();
                    break;
                }
            }
            if (bcoUserId.isEmpty()) {
                throw new NotAvailableException("BCOUser");
            }
            SessionManager.getInstance().login(bcoUserId);
        }
        SessionManager.getInstance().registerClient(id);
    }
}
