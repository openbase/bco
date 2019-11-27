package org.openbase.bco.dal.control.layer.unit.user;

/*
 * #%L
 * BCO DAL Control
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

import org.openbase.bco.dal.control.layer.unit.UnitControllerRegistrySynchronizer;
import org.openbase.bco.dal.control.layer.unit.authorizationgroup.AuthorizationGroupControllerFactoryImpl;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.dal.lib.layer.unit.authorizationgroup.AuthorizationGroupController;
import org.openbase.bco.dal.lib.layer.unit.authorizationgroup.AuthorizationGroupControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.user.UserController;
import org.openbase.bco.dal.lib.layer.unit.user.UserControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.user.UserManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserManagerImpl implements UserManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(UserManagerImpl.class);

    private final UserControllerFactory userControllerFactory;
    private final AuthorizationGroupControllerFactory authorizationGroupControllerFactory;

    private final UnitControllerRegistry<UserController> userControllerRegistry;
    private final UnitControllerRegistry<AuthorizationGroupController> authorizationGroupControllerRegistry;

    private final UnitControllerRegistrySynchronizer<UserController> userRegistrySynchronizer;
    private final UnitControllerRegistrySynchronizer<AuthorizationGroupController> authorizationGroupRegistrySynchronizer;

    public UserManagerImpl() throws org.openbase.jul.exception.InstantiationException {
        try {
            this.userControllerFactory = UserControllerFactoryImpl.getInstance();
            this.authorizationGroupControllerFactory = AuthorizationGroupControllerFactoryImpl.getInstance();

            this.userControllerRegistry = new UnitControllerRegistryImpl<>();
            this.authorizationGroupControllerRegistry = new UnitControllerRegistryImpl<>();

            this.userRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(userControllerRegistry, Registries.getUnitRegistry().getUserUnitConfigRemoteRegistry(false), userControllerFactory);
            this.authorizationGroupRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(authorizationGroupControllerRegistry, Registries.getUnitRegistry().getAuthorizationGroupUnitConfigRemoteRegistry(false), authorizationGroupControllerFactory);
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
        BCOLogin.getSession().loginBCOUser();

        userControllerRegistry.activate();
        authorizationGroupControllerRegistry.activate();

        userRegistrySynchronizer.activate();
        authorizationGroupRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return userControllerRegistry.isActive() &&
                authorizationGroupControllerRegistry.isActive() &&
                userRegistrySynchronizer.isActive() &&
                authorizationGroupRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        userRegistrySynchronizer.deactivate();
        authorizationGroupRegistrySynchronizer.deactivate();

        userControllerRegistry.deactivate();
        authorizationGroupControllerRegistry.deactivate();
    }

    @Override
    public void shutdown() {
        userRegistrySynchronizer.shutdown();
        authorizationGroupRegistrySynchronizer.shutdown();

        userControllerRegistry.shutdown();
        authorizationGroupControllerRegistry.shutdown();
    }

    @Override
    public UnitControllerRegistry<UserController> getUserControllerRegistry() {
        return userControllerRegistry;
    }

    @Override
    public UnitControllerRegistry<AuthorizationGroupController> getAuthorizationGroupControllerRegistry() {
        return authorizationGroupControllerRegistry;
    }
}
