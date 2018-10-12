package org.openbase.bco.manager.app.core;

/*
 * #%L
 * BCO Manager App Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.manager.app.lib.AppController;
import org.openbase.bco.manager.app.lib.AppControllerFactory;
import org.openbase.bco.manager.app.lib.AppManager;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppManagerImpl implements AppManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AppManagerImpl.class);

    private final AppControllerFactory factory;
    private final UnitControllerRegistry<AppController> appControllerRegistry;
    private final ActivatableEntryRegistrySynchronizer<String, AppController, UnitConfig, UnitConfig.Builder> appRegistrySynchronizer;

    public AppManagerImpl() throws org.openbase.jul.exception.InstantiationException {
        try {
            this.factory = AppControllerFactoryImpl.getInstance();
            this.appControllerRegistry = new UnitControllerRegistryImpl<>();

            this.appRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, AppController, UnitConfig, Builder>(appControllerRegistry, Registries.getUnitRegistry().getAppUnitConfigRemoteRegistry(), Registries.getUnitRegistry(), factory) {

                @Override
                public boolean activationCondition(UnitConfig config) {
                    return UnitConfigProcessor.isEnabled(config);
                }
            };

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
        appRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return appRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        appRegistrySynchronizer.deactivate();
    }

    @Override
    public UnitControllerRegistry<AppController> getAppControllerRegistry() {
        return appControllerRegistry;
    }

    @Override
    public void shutdown() {
        appRegistrySynchronizer.shutdown();
    }


    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        throw new NotAvailableException("OperationServiceFactory", new NotSupportedException("OperationServiceFactory", this));
    }
}
