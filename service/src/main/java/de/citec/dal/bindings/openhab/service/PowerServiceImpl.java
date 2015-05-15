/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author mpohling
 * @param <ST> Related service type.
 */
public class PowerServiceImpl<ST extends PowerService & Unit> extends OpenHABService<ST> implements de.citec.dal.hal.service.PowerService {

    public PowerServiceImpl(Device device, ST unit) throws InstantiationException {
        super(device, unit);
    }

    @Override
    public PowerState getPower() throws CouldNotPerformException {
        return unit.getPower();
    }

    @Override
    public void setPower(PowerState.State state) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newOnOffCommand(state));
    }
}
