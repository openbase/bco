package org.openbase.bco.app.openapiserver;

/*-
 * #%L
 * BCO OpenAPI Server
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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


import org.openbase.bco.app.openapiserver.spring.RegistryApiController;
import org.openbase.bco.app.openapiserver.spring.UnitApiController;
import org.openbase.bco.app.openapiserver.spring.converter.StringToServiceTempusConverter;
import org.openbase.bco.app.openapiserver.spring.converter.StringToServiceTypeConverter;
import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;

@Configuration
@EnableAutoConfiguration
@Import({RegistryApiController.class, UnitApiController.class, StringToServiceTempusConverter.class, StringToServiceTypeConverter.class})
public class OpenApiServerApp extends AbstractAppController {

    public static final String KEY_PORT = "PORT";
    private static final String DEFAULT_PORT = "8080";

    private final SpringApplication springApplication;
    private ConfigurableApplicationContext applicationContext;

    public OpenApiServerApp() throws InstantiationException {
        this.springApplication = new SpringApplication(OpenApiServerApp.class);
    }

    @Override
    public UnitConfigType.UnitConfig applyConfigUpdate(UnitConfigType.UnitConfig config) throws CouldNotPerformException, InterruptedException {
        config = super.applyConfigUpdate(config);

        // setup port from meta config
        final MetaConfigPool metaConfigPool = new MetaConfigPool();
        metaConfigPool.register(new MetaConfigVariableProvider("OpenApiAppConfig", config.getMetaConfig()));
        final String port = metaConfigPool.getValue(KEY_PORT, DEFAULT_PORT);
        springApplication.setDefaultProperties(Collections.singletonMap("server.port", port));
        return config;
    }

    @Override
    protected ActionDescription execute(ActivationState activationState) {
        applicationContext = springApplication.run();
        return activationState.getResponsibleAction();
    }

    @Override
    protected void stop(ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        if (applicationContext != null) {
            applicationContext.close();
            applicationContext = null;
        }

        super.stop(activationState);
    }
}
