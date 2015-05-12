/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.dal.hal.device.DeviceFactory;
import de.citec.dal.util.DeviceInitializer;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceManagerRemoteDalConnector implements DeviceInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManagerRemoteDalConnector.class);
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final DeviceFactory factory;

    public DeviceManagerRemoteDalConnector() throws InstantiationException {
        try {
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.factory = new DeviceFactory();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void initDevices(final DeviceRegistry registry) throws CouldNotPerformException {
        MultiException.ExceptionStack exceptionStack = null;
        try {
            logger.info("Init devices...");
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                @Override
                public void run() {
                    deviceRegistryRemote.shutdown();
                }
            }));
            logger.info("Request registry sync.");
            deviceRegistryRemote.requestStatus();
            for (DeviceConfig config : deviceRegistryRemote.getDeviceConfigs()) {
                try {
                    registry.register(factory.newDevice(config));
                } catch (Exception ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            logger.info(registry.size()+" devices successfully loaded. "+MultiException.size(exceptionStack) + " skipped.");
            MultiException.checkAndThrow("Could not init all registered devices!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not init devices!", ex);
        }
    }
}
