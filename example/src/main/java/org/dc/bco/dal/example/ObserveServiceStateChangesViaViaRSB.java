/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.example;

/*
 * #%L
 * DAL Example
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.jps.core.JPService;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.Factory;
import rsb.Handler;
import rsb.Listener;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.RemoteServer;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.AmbientLightType.AmbientLight;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class ObserveServiceStateChangesViaViaRSB {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(DeviceRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter(AmbientLight.getDefaultInstance()));
    }

    public static final String APP_NAME = "CollectionServiceDataViaRemoteLib";

    private static final Logger logger = LoggerFactory.getLogger(ObserveServiceStateChangesViaViaRSB.class);

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.parseAndExitOnError(args);

        ServiceType serviceType = ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_SERVICE;
        List<UnitConfig> unitConfigList = new ArrayList<>();

        try {
            final RemoteServer deviceRegistryRemoteServer = Factory.getInstance().createRemoteServer("/registry/device/ctrl");
            deviceRegistryRemoteServer.activate();
            final DeviceRegistry deviceRegistry = (DeviceRegistry) deviceRegistryRemoteServer.call("requestStatus").getData();

            for (DeviceConfig deviceConfig : deviceRegistry.getDeviceConfigList()) {
                for (UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {
                    for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                        if (serviceConfig.getType().equals(serviceType)) {
                            unitConfigList.add(unitConfig);
                        }
                    }
                }
            }

            Listener listener;
            Scope scope;

            for (UnitConfig unitConfig : unitConfigList) {
                scope = ScopeTransformer.transform(unitConfig.getScope()).concat(new Scope("/status"));
                logger.info("Register listener on Scope[" + scope + "]");
                listener = Factory.getInstance().createListener(scope);
                listener.addHandler(new Handler() {

                    @Override
                    public void internalNotify(Event event) {
                        logger.info("Got Event[" + event.getData() + "]");
                    }
                }, true);
                listener.activate();
            }

        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
        logger.info("Waiting for " + serviceType.name() + " events...");
    }
}
