package org.openbase.bco.dal.example;

/*
 * #%L
 * BCO DAL Example
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.Factory;
import rsb.Listener;
import rsb.RSBException;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.RemoteServer;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;

/**
 *
 * This howto shows how to observe service state changes of any units providing the given service.
 *
 * Note: This howto requires a running bco platform provided by your network.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToObserveServiceStateChangesViaRSB {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(UnitRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(ColorableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(ColorableLightData.getDefaultInstance()));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToObserveServiceStateChangesViaRSB.class);

    public static void howto() throws InterruptedException {

        final ArrayList<Listener> listenerList = new ArrayList<>();

        try {
            // choose your service type to listen
            ServiceType serviceType = ServiceType.COLOR_STATE_SERVICE;

            LOGGER.info("create and activate registry remote server...");
            final RemoteServer unitRegistryRemoteServer = Factory.getInstance().createRemoteServer("/registry/unit/ctrl");
            unitRegistryRemoteServer.activate();

            LOGGER.info("request unit confis...");
            final UnitRegistryData unitRegistryData = (UnitRegistryData) unitRegistryRemoteServer.call("requestStatus").getData();

            // request and iterate over all unit configs
            for (final UnitConfig unitConfig : unitRegistryData.getDalUnitConfigList()) {
                // iterate over provided services
                for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {

                    // check if service type match
                    if (serviceConfig.getServiceTemplate().getType().equals(serviceType)) {
                        final Scope scope = ScopeTransformer.transform(unitConfig.getScope()).concat(new Scope("/status"));
                        LOGGER.info("Register listener on Scope[" + scope + "]");
                        final Listener listener = Factory.getInstance().createListener(scope);
                        listenerList.add(listener);
                        listener.addHandler((Event event) -> {
                            LOGGER.info("Got Event[" + event.getData() + "]");
                        }, true);
                        listener.activate();
                    }
                }
            }

            LOGGER.info("Receiving " + serviceType.name() + " events for one minute...");
            Thread.sleep(60000);
        } catch (RSBException | CouldNotPerformException | ExecutionException | TimeoutException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        } finally {
            LOGGER.info("shutdown listeners");
            for (Listener listener : listenerList) {
                try {
                    listener.deactivate();
                } catch (RSBException ex) {
                    LOGGER.error("Could not shutdown listener!", ex);
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // Setup CLParser
        JPService.setApplicationName("howto");
        JPService.parseAndExitOnError(args);

        // Start HowTo
        LOGGER.info("start " + JPService.getApplicationName());
        howto();
        LOGGER.info("finish " + JPService.getApplicationName());
        System.exit(0);
    }
}
