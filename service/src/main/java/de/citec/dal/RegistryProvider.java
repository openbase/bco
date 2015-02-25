/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.bindings.DALBindingRegistry;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.dal.registry.UnitRegistry;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
    public interface RegistryProvider {

	public DeviceRegistry getDeviceRegistry();
	public UnitRegistry getUnitRegistry();
    public DALBindingRegistry getBindingRegistry();
}
