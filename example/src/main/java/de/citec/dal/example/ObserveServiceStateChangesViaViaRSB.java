/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.example;

import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.rsb.scope.ScopeTransformer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.Factory;
import rsb.Handler;
import rsb.Listener;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.RemoteServer;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.device.philips.Philips_KV01_18YType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class ObserveServiceStateChangesViaViaRSB {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
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
        
        ServiceType serviceType = ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_PROVIDER;
        List<UnitConfig> unitConfigList = new ArrayList<>();

        try {
            final RemoteServer deviceRegistryRemoteServer = Factory.getInstance().createRemoteServer("/devicemanager/registry/ctrl");
            deviceRegistryRemoteServer.activate();
            final DeviceRegistry deviceRegistry = (DeviceRegistry) deviceRegistryRemoteServer.call("requestStatus").getData();
            
            for (DeviceConfig deviceConfig : deviceRegistry.getDeviceConfigList()) {
                for (UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {
                    for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                        if(serviceConfig.getType().equals(serviceType)) {
                            unitConfigList.add(unitConfig);
                        }
                    }
                }
            }
        
            Listener listener;
            
            for (UnitConfig unitConfig : unitConfigList) {
                listener = Factory.getInstance().createListener(ScopeTransformer.transform(unitConfig.getScope()));
                listener.activate();
                listener.addHandler(new Handler() {

                    @Override
                    public void internalNotify(Event event) {
                        System.out.println("Got Event["+event+"]");
                    }
                }, true);
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }

    }
}
