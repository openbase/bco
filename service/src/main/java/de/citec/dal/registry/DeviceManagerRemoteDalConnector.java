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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DeviceManagerRemoteDalConnector implements DeviceInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManagerRemoteDalConnector.class);
    private final DeviceRegistry deviceRegistry;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final DeviceFactory factory;

    public DeviceManagerRemoteDalConnector(final DeviceRegistry deviceRegistry) throws InstantiationException {
        try {
            this.deviceRegistry = deviceRegistry;
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.factory = new DeviceFactory();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }    

    @Override
    public void initDevices(final DeviceRegistry registry) {
        try {
            logger.info("Init devices...");
            deviceRegistryRemote.init();
            deviceRegistryRemote.requestStatus();
//            for(UnitConfigType.UnitConfig unitConfig : deviceRegistryRemote.getUnitConfigs()) {
//               registry.register(factory.newDevice(uni
//
//                   @Override
//                   public String getName() {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//
//                   @Override
//                   public String getLabel() {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//
//                   @Override
//                   public Location getLocation() {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//
//                   @Override
//                   public ServiceFactory getDefaultServiceFactory() {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//
//                   @Override
//                   public Scope getScope() {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//
//                   @Override
//                   public String getId() throws CouldNotPerformException {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//
//                   @Override
//                   public void activate() throws CouldNotPerformException {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//
//                   @Override
//                   public void deactivate() throws CouldNotPerformException, InterruptedException {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//
//                   @Override
//                   public boolean isActive() {
//                       throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//                   }
//               })
            }
        } catch (CouldNotPerformException ex) {
            logger.warn("Could not register devices!", ex);
        }
    }
}
