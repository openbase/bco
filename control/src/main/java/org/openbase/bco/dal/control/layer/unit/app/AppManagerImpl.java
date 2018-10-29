package org.openbase.bco.dal.control.layer.unit.app;

/*
 * #%L
 * BCO DAL Control
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

import org.openbase.bco.dal.control.layer.unit.UnitControllerRegistrySynchronizer;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.dal.lib.layer.unit.app.AppController;
import org.openbase.bco.dal.lib.layer.unit.app.AppControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.app.AppManager;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppManagerImpl implements AppManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AppManagerImpl.class);

    private final AppControllerFactory factory;
    private final UnitControllerRegistry<AppController> appControllerRegistry;
    private final UnitControllerRegistrySynchronizer<AppController> appRegistrySynchronizer;

    public AppManagerImpl() throws InstantiationException {
        try {
            this.factory = AppControllerFactoryImpl.getInstance();
            this.appControllerRegistry = new UnitControllerRegistryImpl<>();
            this.appRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(appControllerRegistry, Registries.getUnitRegistry().getAppUnitConfigRemoteRegistry(),factory);
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() {
        // this has to stay, else do not implement VoidInitializable
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        BCOLogin.loginBCOUser();
        appControllerRegistry.activate();
        appRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return appRegistrySynchronizer.isActive() &&
                appControllerRegistry.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        appRegistrySynchronizer.deactivate();
        appControllerRegistry.deactivate();
    }

    @Override
    public void shutdown() {
        appRegistrySynchronizer.shutdown();
        appControllerRegistry.shutdown();
    }

    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        throw new NotAvailableException("OperationServiceFactory", new NotSupportedException("OperationServiceFactory", this));
    }

    @Override
    public UnitControllerRegistry<AppController> getAppControllerRegistry() {
        return appControllerRegistry;
    }

}
