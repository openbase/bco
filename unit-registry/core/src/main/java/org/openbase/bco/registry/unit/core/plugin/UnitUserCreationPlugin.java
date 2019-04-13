package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig.MapFieldEntry;
import org.openbase.type.domotic.authentication.PermissionType.Permission;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * This plugin creates a user for every agent and app. It also makes sure that they have access and read permissions
 * on their location.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitUserCreationPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    public static final String UNIT_ID_KEY = "UNIT_ID";

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitUserCreationPlugin.class);

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
                UnitConfig userUnitConfig;

                try {
                    userUnitConfig = findUser(unitConfig);
                    if (!CachedAuthenticationRemote.getRemote().hasUser(userUnitConfig.getId()).get()) {
                        registerUserAtAuthenticator(userUnitConfig.getId());
                    }
                } catch (NotAvailableException ex) {
                    userUnitConfig = registerUser(unitConfig);
                }

                addLocationPermissions(userUnitConfig, unitConfig);
            }
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void beforeRemove(final IdentifiableMessage<String, UnitConfig, Builder> unitConfig) throws RejectedException {
        try {
            // find user belonging to the unit
            final UnitConfig user = findUser(unitConfig.getMessage());

            // remove user
            userRegistry.remove(user);
            //TODO: also remove user from authenticator... currently only possible as admin
        } catch (CouldNotPerformException e) {
            throw new RejectedException("Could not remove user of unit[" + unitConfig.getMessage().getAlias(0) + "]");
        }
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, UnitConfig, Builder> unitConfig) throws CouldNotPerformException {
        final UnitConfig userConfig = registerUser(unitConfig.getMessage());

        addLocationPermissions(userConfig, unitConfig.getMessage());
    }

    @Override
    public void afterUpdate(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        try {
            addLocationPermissions(findUser(identifiableMessage.getId(), userRegistry), identifiableMessage.getMessage());
        } catch (NotAvailableException ex) {
            // do nothing because user has not yet been registered
            // this method is also called if the entry is modified through consistency checks and thus can be called before afterRegisters
        }
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
            final MetaConfigPool metaConfigPool = new MetaConfigPool();
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

    private UnitConfig registerUser(final UnitConfig unitConfig) throws CouldNotPerformException {
        final UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder();
        userUnitConfig.setUnitType(UnitType.USER);
        final Entry.Builder entry = userUnitConfig.getMetaConfigBuilder().addEntryBuilder();
        entry.setKey(UNIT_ID_KEY).setValue(unitConfig.getId());
        final UserConfig.Builder userConfig = userUnitConfig.getUserConfigBuilder();
        userConfig.setSystemUser(true);
        userConfig.setUserName(getUsername(unitConfig));
        final UnitConfig registeredUserConfig = userRegistry.register(userUnitConfig.build());
        registerUserAtAuthenticator(registeredUserConfig.getId());
        return registeredUserConfig;
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
            SessionManager.getInstance().loginClient(bcoUserId, true);
        }
        try {
            SessionManager.getInstance().registerClient(id).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not register user " + id + " because of interruption", ex);
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register user " + id, ex);
        }
    }

    private static Permission RX_PERMISSION = Permission.newBuilder().setRead(true).setAccess(true).setWrite(false).build();

    private void addLocationPermissions(final UnitConfig userConfig, final UnitConfig unitConfig) throws CouldNotPerformException {
        // retrieve location of unit
        final UnitConfig.Builder location = locationRegistry.getMessage(unitConfig.getPlacementConfig().getLocationId()).toBuilder();

        // add permission for user
        final PermissionConfig.Builder permissionConfigBuilder = location.getPermissionConfigBuilder();
        MapFieldEntry.Builder groupPermissionBuilder = null;

        // check if the user already has the expected permissions
        for (int i = 0; i < permissionConfigBuilder.getGroupPermissionCount(); i++) {
            if (permissionConfigBuilder.getGroupPermission(i).getGroupId().equals(userConfig.getId())) {
                groupPermissionBuilder = permissionConfigBuilder.getGroupPermissionBuilder(i);
                if (groupPermissionBuilder.getPermission().equals(RX_PERMISSION)) {
                    // return because the permissions are correctly set
                    return;
                }
            }
        }

        // user does not have a permission entry yet so add one
        if (groupPermissionBuilder == null) {
            groupPermissionBuilder = permissionConfigBuilder.addGroupPermissionBuilder();
        }
        // set access and read permissions
        groupPermissionBuilder.setGroupId(userConfig.getId()).setPermission(RX_PERMISSION);

        // update location
        locationRegistry.update(location.build());
    }
}
