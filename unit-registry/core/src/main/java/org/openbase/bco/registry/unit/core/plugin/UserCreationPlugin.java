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

import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig;
import org.openbase.type.domotic.authentication.PermissionType.Permission;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class UserCreationPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private static final String ADMIN_USERNAME = "Admin";
    private static final String BCO_USERNAME = "BCO";
    public static final String ADMIN_PASSWORD = "admin";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UserCreationPlugin.class);

    private final Map<String, String> idAliasMap;

    public UserCreationPlugin() {
        this.idAliasMap = new HashMap<>();
    }

    @Override
    public void init(final ProtoBufRegistry<String, UnitConfig, Builder> userRegistry) throws InitializationException, InterruptedException {
        super.init(userRegistry);

        try {
            UnitConfig admin;
            try {
                admin = getUserByAlias(UnitRegistry.ADMIN_USER_ALIAS);
            } catch (NotAvailableException ex) {
                // admin not registered at all
                admin = registerAdmin();
            }

            if (!CachedAuthenticationRemote.getRemote().isAdmin(admin.getId()).get(3, TimeUnit.SECONDS)) {
                // admin registered in registry but not at authenticator
                registerAdminAtAuthenticator(admin.getId());
            }

            UnitConfig bco;
            try {
                bco = getUserByAlias(UnitRegistry.BCO_USER_ALIAS);
            } catch (NotAvailableException ex) {
                // bco user not registered at all
                bco = registerBCO();
            }

            if (!CachedAuthenticationRemote.getRemote().hasUser(bco.getId()).get(3, TimeUnit.SECONDS)) {
                // bco user registered in registry but not at the authenticator
                registerBCOAtAuthenticator(bco.getId(), admin.getId());
            }

            idAliasMap.put(admin.getId(), UnitRegistry.ADMIN_USER_ALIAS);
            idAliasMap.put(bco.getId(), UnitRegistry.BCO_USER_ALIAS);
        } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
            throw new InitializationException(this, new CouldNotPerformException("Could not validate or register initial user accounts", ex));
        }
    }

    private UnitConfig getUserByAlias(final String alias) throws CouldNotPerformException {
        for (UnitConfig unitConfig : getRegistry().getMessages()) {
            for (final String unitAlias : unitConfig.getAliasList()) {
                if (unitAlias.equals(alias)) {
                    return unitConfig;
                }
            }
        }
        throw new NotAvailableException("User with alias[" + alias + "]");
    }

    @Override
    public void beforeUpdate(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws RejectedException {
        final UnitConfig user = identifiableMessage.getMessage();
        if (!idAliasMap.containsKey(user.getId())) {
            return;
        }

        final String expectedAlias = idAliasMap.get(user.getId());
        for (final String alias : user.getAliasList()) {
            if (alias.equals(expectedAlias)) {
                return;
            }
        }

        throw new RejectedException("User[" + user.getId() + "] should contain the alias[" + expectedAlias + "]");
    }

    @Override
    public void beforeRemove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws RejectedException {
        final String id = identifiableMessage.getMessage().getId();
        if (idAliasMap.containsKey(id)) {
            throw new RejectedException("User[" + identifiableMessage.getMessage().getId() + "] cannot be removed because it is the " + idAliasMap.get(id));
        }
    }


    private void registerAdminAtAuthenticator(final String adminId) throws CouldNotPerformException {
        final String initialRegistrationPassword = AuthenticatorController.getInitialPassword();

        // verify initial password
        if (initialRegistrationPassword == null) {
            LOGGER.error("The initial registration password is not available even though no initial admin was registered at the authenticator." +
                    "This means that either the authenticator has not been started in the same process or that a user is already registered which could not be identified as the default admin." +
                    "Please verify that you used to bco launcher and reset all credentials with '--reset-credentials'");

            // init shutdown
            new Thread(() -> System.exit(1)).start();

            throw new NotAvailableException("initial registration password");
        }

        // register at authenticator
        final LoginCredentialsChange.Builder loginCredentials = LoginCredentialsChange.newBuilder().setId(adminId);
        loginCredentials.setNewCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash(ADMIN_PASSWORD), EncryptionHelper.hash(initialRegistrationPassword)));

        try {
            CachedAuthenticationRemote.getRemote().register(loginCredentials.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register default administrator at authenticator");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private UnitConfig registerAdmin() throws CouldNotPerformException {
        final UnitConfig.Builder admin = UnitConfig.newBuilder();
        admin.setUnitType(UnitType.USER).addAlias(UnitRegistry.ADMIN_USER_ALIAS);

        PermissionConfig.Builder permissionConfig = admin.getPermissionConfigBuilder();
        Permission.Builder otherPermission = permissionConfig.getOtherPermissionBuilder();
        otherPermission.setRead(true).setAccess(true).setWrite(true);

        UserConfig.Builder userConfig = admin.getUserConfigBuilder();
        userConfig.setFirstName("Initial");
        userConfig.setLastName("Admin");
        userConfig.setUserName(ADMIN_USERNAME);

        return getRegistry().register(admin.build());
    }

    private UnitConfig registerBCO() throws CouldNotPerformException {
        final UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
        unitConfig.setUnitType(UnitType.USER).addAlias(UnitRegistry.BCO_USER_ALIAS);

        final PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();
        permissionConfig.getOtherPermissionBuilder().setRead(true).setAccess(true).setWrite(true);

        final UserConfig.Builder userConfig = unitConfig.getUserConfigBuilder();
        userConfig.setFirstName("System");
        userConfig.setLastName("User");
        userConfig.setUserName(BCO_USERNAME);
        userConfig.setSystemUser(true);

        return getRegistry().register(unitConfig.build());
    }

    private void registerBCOAtAuthenticator(final String bcoId, final String adminId) throws CouldNotPerformException {
        // login as admin
        try {
            SessionManager.getInstance().login(adminId, ADMIN_PASSWORD);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not login as the default admin to register the bco user at the authenticator");
        }

        // register bco user as a client at the authenticator
        SessionManager.getInstance().registerClient(bcoId);

        // logout
        SessionManager.getInstance().logout();
    }
}
