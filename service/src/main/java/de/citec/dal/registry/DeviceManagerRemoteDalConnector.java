/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.DALService;
import de.citec.dal.hal.device.DeviceFactory;
import de.citec.dal.util.DeviceInitializer;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.state.InventoryStateType;

/**
 *
 * @author mpohling
 */
public class DeviceManagerRemoteDalConnector implements DeviceInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManagerRemoteDalConnector.class);
    private final DeviceFactory factory;

    public DeviceManagerRemoteDalConnector() throws InstantiationException {
        try {
            this.factory = new DeviceFactory();
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void initDevices(final DeviceRegistry registry) throws CouldNotPerformException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        DeviceRegistryRemote deviceRegistryRemote = DALService.getRegistryProvider().getDeviceRegistryRemote();
        
        while (true) {
            try {
                logger.info("Request registry sync.");
                deviceRegistryRemote.requestStatus();
                for (DeviceConfig config : deviceRegistryRemote.getDeviceConfigs()) {
                    
                    if(!config.getDeviceClass().getBindingConfig().getType().equals(BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB)) {
                        continue;
                    }
                    
                    if(config.getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                        logger.info("Skip Device["+config.getLabel()+"] because it is currently not installed!");
                        continue;
                    }
                    
                    try {
                        registry.register(factory.newDevice(config));
                    } catch (Exception ex) {
                        exceptionStack = MultiException.push(this, ex, exceptionStack);
                    }
                }

                MultiException.checkAndThrow("Could not init all registered devices!", exceptionStack);
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not init devices!", ex);
            }
            if (registry.isEmpty()) {
                logger.warn("No devices found... try again in 30 sec..");
                Thread.sleep(30000);
                continue;
            }
            break;
        }
        logger.info(registry.size() + " devices successfully loaded. " + MultiException.size(exceptionStack) + " skipped.");
    }
}
