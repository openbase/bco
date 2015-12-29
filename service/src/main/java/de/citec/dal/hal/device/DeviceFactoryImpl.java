/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceFactoryImpl extends AbstractDeviceFactory {

    public DeviceFactoryImpl(final DeviceRegistryRemote deviceRegistryRemote) {
        super(deviceRegistryRemote);
    }

    @Override
    public Device newInstance(final DeviceConfig deviceConfig, final DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            if (deviceClass == null) {
                throw new NotAvailableException("deviceClass");
            }

            if (!deviceClass.hasCompany()) {
                throw new NotAvailableException("deviceClass.company");
            }

            if (deviceConfig == null) {
                throw new NotAvailableException("deviceConfig");
            }

            if (!deviceConfig.hasId()) {
                throw new NotAvailableException("deviceConfig.id");
            }

            if (!deviceConfig.hasLabel()) {
                throw new NotAvailableException("deviceConfig.label");
            }

            if (!deviceConfig.hasPlacementConfig()) {
                throw new NotAvailableException("deviceConfig.placement");
            }

            if (!deviceConfig.getPlacementConfig().hasLocationId()) {
                throw new NotAvailableException("deviceConfig.placement.locationId");
            }

            return new GenericDeviceController(deviceConfig);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not instantiate Device[" + deviceConfig.getId() + "]!", ex);
        }
    }
}
