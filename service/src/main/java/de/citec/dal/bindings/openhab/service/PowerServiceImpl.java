/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.unit.UnitInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 * @param <ST> Related service type.
 */
public class PowerServiceImpl<ST extends PowerService & UnitInterface> extends OpenHABService<ST> implements de.citec.dal.hal.service.PowerService {

    public PowerServiceImpl(DeviceInterface device, ST unit) {
        super(device, unit);
    }

    @Override
    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException {
        return unit.getPowerState();
    }

    @Override
    public void setPowerState(PowerType.Power.PowerState state) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newOnOffCommand(state));
    }
}
