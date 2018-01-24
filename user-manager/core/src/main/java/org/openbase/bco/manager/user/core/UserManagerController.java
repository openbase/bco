package org.openbase.bco.manager.user.core;

/*
 * #%L
 * BCO Manager User Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.manager.user.lib.*;
import org.openbase.bco.registry.login.SystemLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.openbase.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.EnablingStateType.EnablingState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserManagerController implements UserManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(UserManagerController.class);

    private final UserFactory userFactory;
    private final AuthorizationGroupFactory authorizationGroupFactory;

    private final ControllerRegistryImpl<String, UserController> userRegistry;
    private final ControllerRegistryImpl<String, AuthorizationGroupController> authorizationGroupRegistry;

    private final EnableableEntryRegistrySynchronizer<String, UserController, UnitConfig, UnitConfig.Builder> userRegistrySynchronizer;
    private final ActivatableEntryRegistrySynchronizer<String, AuthorizationGroupController, UnitConfig, UnitConfig.Builder> authorizationGroupRegistrySynchronizer;

    public UserManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.userFactory = UserFactoryImpl.getInstance();
            this.authorizationGroupFactory = AuthorizationGroupFactoryImpl.getInstance();

            this.userRegistry = new ControllerRegistryImpl<>();
            this.authorizationGroupRegistry = new ControllerRegistryImpl<>();

            this.userRegistrySynchronizer = new EnableableEntryRegistrySynchronizer<String, UserController, UnitConfig, UnitConfig.Builder>(userRegistry, Registries.getUserRegistry().getUserConfigRemoteRegistry(), Registries.getUserRegistry(), userFactory) {

                @Override
                public boolean enablingCondition(UnitConfig config) {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };
            this.authorizationGroupRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, AuthorizationGroupController, UnitConfig, Builder>(authorizationGroupRegistry, Registries.getUnitRegistry().getAuthorizationGroupUnitConfigRemoteRegistry(), Registries.getUserRegistry(), authorizationGroupFactory) {
                @Override
                public boolean activationCondition(UnitConfig config) {
                    return config.getEnablingState().getValue() == State.ENABLED;
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        // This has to stay. Else do not implement VoidInitializable. 
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        Registries.getUserRegistry().waitForData();

        SystemLogin.loginBCOUser();

        userRegistrySynchronizer.activate();
        authorizationGroupRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return userRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        userRegistrySynchronizer.deactivate();
        authorizationGroupRegistrySynchronizer.deactivate();

        userRegistry.clear();
        authorizationGroupRegistry.clear();
    }

    @Override
    public void shutdown() {
        userRegistrySynchronizer.shutdown();
        authorizationGroupRegistrySynchronizer.shutdown();

        userRegistry.shutdown();
        authorizationGroupRegistry.shutdown();
    }
}
