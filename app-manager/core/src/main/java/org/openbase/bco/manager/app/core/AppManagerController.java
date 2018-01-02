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
import org.openbase.bco.manager.app.lib.AppController;
import org.openbase.bco.manager.app.lib.AppFactory;
import org.openbase.bco.manager.app.lib.AppManager;
import org.openbase.bco.registry.login.SystemLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.openbase.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppManagerController implements AppManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AppManagerController.class);

    private final AppFactory factory;
    private final ControllerRegistryImpl<String, AppController> appRegistry;
    private final EnableableEntryRegistrySynchronizer<String, AppController, UnitConfig, UnitConfig.Builder> appRegistrySynchronizer;

    public AppManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.factory = AppFactoryImpl.getInstance();
            this.appRegistry = new ControllerRegistryImpl<>();

            this.appRegistrySynchronizer = new EnableableEntryRegistrySynchronizer<String, AppController, UnitConfig, UnitConfig.Builder>(appRegistry, Registries.getUnitRegistry().getAppUnitConfigRemoteRegistry(), Registries.getUnitRegistry(), factory) {

                @Override
                public boolean enablingCondition(final UnitConfig config) {
                    return config.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED;
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        //TODO: why is this necessary
        Registries.waitForData();
        SystemLogin.loginBCOUser();
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
    public void shutdown() {
        appRegistrySynchronizer.shutdown();
    }
}
