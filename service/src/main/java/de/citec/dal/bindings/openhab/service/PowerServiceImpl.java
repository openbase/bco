/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABServiceImpl;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 */
public class PowerServiceImpl extends OpenHABServiceImpl<de.citec.dal.hal.service.PowerService> implements de.citec.dal.hal.service.PowerService {

    public PowerServiceImpl(AbstractDeviceController device, de.citec.dal.hal.service.PowerService unit) {
        super(device, unit);
    }

    @Override
    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException {
        return unit.getPowerState();
    }

    @Override
    public void setPowerState(PowerType.Power.PowerState state) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
