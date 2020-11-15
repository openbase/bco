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

import org.openbase.bco.api.graphql.discovery.ServiceAdvertiser;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceInfo.Fields;
import java.util.HashMap;
import java.util.UUID;

public class BcoApiGraphQlSpringLaunchable implements Launchable<Void>, VoidInitializable {

    private static Logger LOGGER = LoggerFactory.getLogger(BcoApiGraphQlSpringLaunchable.class);
    private  ServiceAdvertiser serviceAdvertiser;


    private ConfigurableApplicationContext context;


    @Override
    public void init() throws InitializationException {
        try {
            serviceAdvertiser = ServiceAdvertiser.getInstance();
        } catch (InstantiationException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        LOGGER.info("Connect to bco...");
        Registries.waitUntilReady();

        LOGGER.info("Login to bco...");
        BCOLogin.getSession().loginBCOUser();

        LOGGER.info("Start webserver...");
        context = SpringApplication.run(BcoGraphQlApiSpringBootApplication.class, JPService.getArgs());

        LOGGER.info("Advertise graphql service...");
        HashMap<Fields, String> qualifiedNameMap = new HashMap<>();
        qualifiedNameMap.put(Fields.Application, "http");
        qualifiedNameMap.put(Fields.Instance, "graphql-bco-openbase");
        qualifiedNameMap.put(Fields.Subtype, "graphql");

        HashMap<String, String> propertyMap = new HashMap<>();
        propertyMap.put("bco-uuid", UUID.randomUUID().toString());
        propertyMap.put("path", "graphql");

        final ServiceInfo info = serviceAdvertiser.register(qualifiedNameMap, 8080, 0, 0, false, propertyMap);
//        LOGGER.info("Service Name: "+info.getQualifiedName());
//        LOGGER.info("Service Type: "+info.getTypeWithSubtype());
    }

    @Override
    public void deactivate() {

        LOGGER.info("Logout...");
        serviceAdvertiser.shutdown();
        BCOLogin.getSession().logout();

        if(isActive()) {
            LOGGER.info("Shutdown "+ context.getApplicationName());
            SpringApplication.exit(context);
            context = null;
        }
    }

    @Override
    public boolean isActive() {
        return context != null && context.isActive();
    }
}
