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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.Factory;
import rsb.Handler;
import rsb.Listener;
import rsb.RSBException;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.RemoteServer;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;
import rst.domotic.registry.DeviceRegistryDataType.DeviceRegistryData;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ObserveServiceStateChangesViaRSB {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(DeviceRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(ColorableLightData.getDefaultInstance()));
    }

    public static final String APP_NAME = "CollectionServiceDataViaRemoteLib";

    private static final Logger logger = LoggerFactory.getLogger(ObserveServiceStateChangesViaRSB.class);

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.parseAndExitOnError(args);

        ServiceType serviceType = ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE;
        List<UnitConfig> unitConfigList = new ArrayList<>();

        try {
            final RemoteServer unitRegistryRemoteServer = Factory.getInstance().createRemoteServer("/registry/unit/ctrl");
            unitRegistryRemoteServer.activate();
            final UnitRegistryData unitRegistryData = (UnitRegistryData) unitRegistryRemoteServer.call("requestStatus").getData();

            // request all units of the given service type
            for (UnitConfig unitConfig : unitRegistryData.getDalUnitConfigList()) {
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceTemplate().getType().equals(serviceType)) {
                        unitConfigList.add(unitConfig);
                    }
                }
            }

            Listener listener;
            Scope scope;

            for (UnitConfig unitConfig : unitConfigList) {
                scope = ScopeTransformer.transform(unitConfig.getScope()).concat(new Scope("/status"));
                logger.info("Register listener on Scope[" + scope + "]");
                listener = Factory.getInstance().createListener(scope);
                listener.addHandler((Event event) -> {
                    logger.info("Got Event[" + event.getData() + "]");
                }, true);
                listener.activate();
            }

        } catch (RSBException | CouldNotPerformException | ExecutionException | TimeoutException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
        logger.info("Waiting for " + serviceType.name() + " events...");
    }
}
