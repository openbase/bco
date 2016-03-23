/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.app.core;

/*
 * #%L
 * COMA AppManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.bco.manager.app.lib.AppController;
import org.dc.bco.manager.app.lib.AppFactory;
import org.dc.bco.manager.app.lib.AppManager;
import org.dc.bco.registry.app.remote.AppRegistryRemote;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.bco.registry.device.lib.provider.DeviceRegistryProvider;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.dc.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.state.EnablingStateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class AppManagerController implements DeviceRegistryProvider, AppManager {

    protected static final Logger logger = LoggerFactory.getLogger(AppManagerController.class);

    private static AppManagerController instance;
    private final AppFactory factory;
    private final RegistryImpl<String, AppController> appRegistry;
    private final AppRegistryRemote appRegistryRemote;
    private final EnableableEntryRegistrySynchronizer<String, AppController, AppConfig, AppConfig.Builder> registrySynchronizer;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public AppManagerController() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;
            this.factory = AppFactoryImpl.getInstance();
            this.appRegistry = new RegistryImpl<>();
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.appRegistryRemote = new AppRegistryRemote();

            this.registrySynchronizer = new EnableableEntryRegistrySynchronizer<String, AppController, AppConfig, AppConfig.Builder>(appRegistry, appRegistryRemote.getAppConfigRemoteRegistry(), factory) {

                @Override
                public boolean enablingCondition(final AppConfig config) {
                    return config.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED;
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public static AppManagerController getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(AppManagerController.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            this.appRegistryRemote.init();
            this.appRegistryRemote.activate();
            this.deviceRegistryRemote.init();
            this.deviceRegistryRemote.activate();
            this.registrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        this.appRegistryRemote.shutdown();
        this.deviceRegistryRemote.shutdown();
        this.deviceRegistryRemote.shutdown();
        instance = null;
    }

    @Override
    public DeviceRegistry getDeviceRegistry() throws NotAvailableException {
        return this.deviceRegistryRemote;
    }
}
