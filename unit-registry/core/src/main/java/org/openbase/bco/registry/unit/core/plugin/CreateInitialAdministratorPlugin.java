package org.openbase.bco.registry.unit.core.plugin;

/*-
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
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class CreateInitialAdministratorPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    public static final String DEFAULT_ADMIN_USERNAME_AND_PASSWORD = "admin";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CreateInitialAdministratorPlugin.class);

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userUnitConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupConfigRegistry;

    public CreateInitialAdministratorPlugin(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> userUnitConfigRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupConfigRegistry) {
        this.userUnitConfigRegistry = userUnitConfigRegistry;
        this.authorizationGroupConfigRegistry = authorizationGroupConfigRegistry;
    }

    @Override
    public void init(Registry<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> registry) throws InitializationException, InterruptedException {
        super.init(registry);

        try {
            UnitConfig.Builder adminGroupConfig = null;
            for (UnitConfig authorizationGroup : authorizationGroupConfigRegistry.getMessages()) {
                if (authorizationGroup.getLabel().equals(AuthorizationGroupCreationPlugin.ADMIN_GROUP_LABEL)) {
                    adminGroupConfig = authorizationGroup.toBuilder();
                    break;
                }
            }
            if (adminGroupConfig == null) {
                throw new InitializationException(this, new NotAvailableException("admin group"));
            }

            for (UnitConfig userUnitConfig : userUnitConfigRegistry.getMessages()) {
                if (CachedAuthenticationRemote.getRemote().isAdmin(userUnitConfig.getId()).get(1, TimeUnit.SECONDS)) {
                    if (adminGroupConfig.getAuthorizationGroupConfig().getMemberIdList().contains(userUnitConfig.getId())) {
                        // if there is at least one admin registered at the authenticator and in the admin group everything is fine
                        return;
                    } else {
                        // user is admin but not in group, so add him
                        AuthorizationGroupConfig.Builder authorizationGroup = adminGroupConfig.getAuthorizationGroupConfigBuilder();
                        authorizationGroup.addMemberId(userUnitConfig.getId());
                        authorizationGroupConfigRegistry.update(adminGroupConfig.build());
                        return;
                    }
                }
            }

            LOGGER.info("No administrator found so try to register a new default one");
            registerDefaultAdmin(adminGroupConfig);
        } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
            throw new InitializationException(this, new CouldNotPerformException("Could not check for existing or register administrator account", ex));
        }
    }

    public void registerDefaultAdmin(UnitConfig.Builder adminGroupConfig) throws CouldNotPerformException {
        String initialRegistrationPassword = AuthenticatorController.getInitialPassword();

        if (initialRegistrationPassword == null) {
            if(JPService.testMode()) {
                return;
            }
            LOGGER.error("No administator is yet registered and the initial registartion password of the authenticator is not available. Please use the bco launcher for the initial start.");
            System.exit(1);
        }

        UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
        unitConfig.setType(UnitType.USER);

        UserConfig.Builder userConfig = unitConfig.getUserConfigBuilder();
        userConfig.setFirstName("Initial");
        userConfig.setLastName("Admin");
        userConfig.setUserName(DEFAULT_ADMIN_USERNAME_AND_PASSWORD);

        String userId = userUnitConfigRegistry.register(unitConfig.build()).getId();

        LoginCredentialsChange.Builder loginCredentials = LoginCredentialsChange.newBuilder();
        loginCredentials.setId(userId);

        try {
            loginCredentials.setNewCredentials(EncryptionHelper.encryptSymmetric(EncryptionHelper.hash(DEFAULT_ADMIN_USERNAME_AND_PASSWORD), EncryptionHelper.hash(initialRegistrationPassword)));
        } catch (IOException ex) {
            throw new CouldNotPerformException("Could not encrypt password", ex);
        }
        try {
            CachedAuthenticationRemote.getRemote().register(loginCredentials.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register default administrator at authenticator");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        AuthorizationGroupConfig.Builder authorizationGroup = adminGroupConfig.getAuthorizationGroupConfigBuilder();
        authorizationGroup.addMemberId(userId);

        authorizationGroupConfigRegistry.update(adminGroupConfig.build());
    }
}
