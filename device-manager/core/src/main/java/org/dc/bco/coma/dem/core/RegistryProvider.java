/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.core;

import de.citec.dal.binding.DALBindingRegistry;
import de.citec.dal.registry.UnitRegistry;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;

/**
 *
 * @author Divine Threepwood
 */
    public interface RegistryProvider {

    public DeviceRegistryRemote getDeviceRegistryRemote();
    public LocationRegistryRemote getLocationRegistryRemote();
	public DeviceRegistry getDeviceRegistry();
	public UnitRegistry getUnitRegistry();
    public DALBindingRegistry getBindingRegistry();
}
