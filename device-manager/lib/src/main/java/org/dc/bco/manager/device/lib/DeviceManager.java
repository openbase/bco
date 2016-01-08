/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.lib;

import org.dc.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.dc.bco.dal.lib.layer.service.ServiceFactoryProvider;
import org.dc.bco.registry.device.lib.provider.DeviceRegistryProvider;
import org.dc.bco.registry.location.lib.provider.LocationRegistryProvider;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author Divine Threepwood
 */
public interface DeviceManager extends LocationRegistryProvider, DeviceRegistryProvider, ServiceFactoryProvider, DeviceFactoryProvider {

    public DeviceControllerRegistry getDeviceControllerRegistry() throws NotAvailableException;

    public UnitControllerRegistry getUnitControllerRegistry() throws NotAvailableException;

    public boolean isSupported(final DeviceConfigType.DeviceConfig config) throws CouldNotPerformException;

}
