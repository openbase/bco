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
import org.openbase.bco.dal.lib.layer.unit.app.App;
import org.openbase.bco.manager.app.lib.AppController;
import org.openbase.bco.manager.app.lib.AppFactory;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.app.AppClassType.AppClass;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppFactoryImpl implements AppFactory {

    protected final Logger logger = LoggerFactory.getLogger(AppFactoryImpl.class);
    private static AppFactoryImpl instance;

    public synchronized static AppFactoryImpl getInstance() {
        if (instance == null) {
            instance = new AppFactoryImpl();
        }
        return instance;
    }

    private AppFactoryImpl() {
    }

    @Override
    public AppController newInstance(final UnitConfig config) throws org.openbase.jul.exception.InstantiationException {
        final AppController app;
        try {
            if (config == null) {
                throw new NotAvailableException("appconfig");
            }

            final Class appClass = Thread.currentThread().getContextClassLoader().loadClass(getAppClass(config));
            logger.debug("Creating app of type [" + appClass.getSimpleName() + "]");
            app = (AppController) appClass.newInstance();
            app.init(config);
        } catch (CouldNotPerformException | ClassNotFoundException | SecurityException | java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InterruptedException ex) {
            throw new org.openbase.jul.exception.InstantiationException(App.class, config.getId(), ex);
        }
        return app;
    }

    private String getAppClass(final UnitConfig appUnitConfig) throws InterruptedException, NotAvailableException {
        try {
            Registries.getAppRegistry().waitForData();
            AppClass appClass = Registries.getAppRegistry().getAppClassById(appUnitConfig.getAppConfig().getAppClassId());
            return AbstractAppController.class.getPackage().getName() + "."
                    + "preset."
                    + appClass.getLabel()
                    + "App";
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("AppClass", ex);
        }
    }
}
