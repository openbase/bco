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


import com.google.protobuf.Message;
import org.openbase.bco.app.openapiserver.spring.ServerConfiguration;
import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class OpenApiServerApp extends AbstractAppController {

    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final CustomUnitPool customUnitPool;
    private final Observer<ServiceStateProvider<Message>, Message> unitStateObserver;
    private final SpringApplication springApplication;
    private ConfigurableApplicationContext applicationContext;
//    private Future task;

    public OpenApiServerApp() throws InstantiationException {
        this.customUnitPool = new CustomUnitPool();
        this.unitStateObserver = (source, data) -> storeServiceState((Unit) source.getServiceProvider(), source.getServiceType(), false);
        this.springApplication = new SpringApplication(ServerConfiguration.class);
    }

    @Override
    public UnitConfigType.UnitConfig applyConfigUpdate(UnitConfigType.UnitConfig config) throws CouldNotPerformException, InterruptedException {
        config = super.applyConfigUpdate(config);
        //TODO: parse from meta config
        springApplication.setDefaultProperties(Collections.singletonMap("server.port", "8080"));
        return config;
    }

    @Override
    protected ActionDescription execute(ActivationState activationState) {
        LOGGER.info("START SERVER");
        //TODO listen for events to make sure it is running (https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-spring-application.html)
        applicationContext = springApplication.run();
//        task = GlobalCachedExecutorService.submit(() -> {
//
//            // try {
//                LOGGER.info("START SERVER");
//            ConfigurableApplicationContext run = SpringApplication.run(ServerConfiguration.class);
//            run.start();
//            // } catch (NotAvailableException ex) {
//            //     ExceptionPrinter.printHistory("Could not start openapi server!", ex, logger, LogLevel.WARN);
//            // }
//            // start observation
//            try {
//                startObservation();
//            } catch (InitializationException ex) {
//                ExceptionPrinter.printHistory(ex, logger);
//            }
//            return null;
//        });
        return activationState.getResponsibleAction();
    }

    @Override
    protected void stop(ActivationState activationState) throws CouldNotPerformException, InterruptedException {

        if (applicationContext != null) {
            //TODO: how to make sure that it stopped
            applicationContext.close();
        }
//        // finish task
//        if (task != null && !task.isDone()) {
//            task.cancel(true);
//            try {
//                task.get(5, TimeUnit.SECONDS);
//            } catch (CancellationException ex) {
//                // that's what we are waiting for.
//            } catch (Exception ex) {
//                ExceptionPrinter.printHistory(ex, logger);
//            }
//        }

        // deregister
        customUnitPool.removeObserver(unitStateObserver);
        customUnitPool.deactivate();

        super.stop(activationState);
    }

    public void startObservation() throws InitializationException, InterruptedException {
        try {
            // setup pool
            customUnitPool.addObserver(unitStateObserver);
            customUnitPool.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void storeServiceState(Unit<?> unit, ServiceTemplateType.ServiceTemplate.ServiceType serviceType, boolean initialSync) throws CouldNotPerformException {
        // TODO: BROADCAST SERVER SEND EVENT
        LOGGER.info("BROADCAST SERVER SEND EVENT");
    }
}
