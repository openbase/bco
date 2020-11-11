package org.openbase.bco.api.graphql;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
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

import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class BcoApiGraphQlSpringLaunchable implements Launchable<Void>, VoidInitializable {

    private static Logger LOGGER = LoggerFactory.getLogger(BcoApiGraphQlSpringLaunchable.class);

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // nothing to initialize
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        LOGGER.info("Connect to bco...");
        Registries.waitUntilReady();

        LOGGER.info("Login to bco...");
        BCOLogin.getSession().loginBCOUser();

        LOGGER.info("Start webserver...");
        context = SpringApplication.run(BcoGraphQlApiSpringBootApplication.class, JPService.getArgs());
    }

    @Override
    public void deactivate() {
        LOGGER.info("Logout...");
        BCOLogin.getSession().logout();

        if(isActive()) {
            LOGGER.info("Shutdown "+ context.getApplicationName());
            SpringApplication.exit(context);
            context = null;
        }
    }

    @Override
    public boolean isActive() {
        return context != null;
    }
}
