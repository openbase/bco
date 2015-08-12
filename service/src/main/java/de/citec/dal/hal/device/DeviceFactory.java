/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.processing.StringProcessor;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceFactory extends AbstractDeviceFactory {

    public DeviceFactory(final DeviceRegistryRemote deviceRegistryRemote) {
        super(deviceRegistryRemote);
    }

    @Override
    public Device newDevice(final DeviceConfig deviceConfig, final DeviceClass deviceClass) throws CouldNotPerformException {
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

            Class deviceControllerClass = getClass().getClassLoader().loadClass(getDeviceControllerClass(deviceClass));
            return (Device) deviceControllerClass.getConstructor(DeviceConfig.class).newInstance(deviceConfig);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not instantiate Device[" + deviceConfig.getId() + "]!", ex);
        }
    }

    private String getDeviceControllerClass(final DeviceClass deviceClass) {
        return AbstractDeviceController.class.getPackage().getName() + "."
                + deviceClass.getCompany().toLowerCase() + "."
                + deviceClass.getCompany() + "_"
                + StringProcessor.replaceHyphenWithUnderscore(deviceClass.getProductNumber())
                + "Controller";
    }
}
