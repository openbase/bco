/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.service;

import org.dc.bco.coma.dem.binding.openhab.OpenHABCommandFactory;
import org.dc.bco.coma.dem.lib.Device;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author mpohling
 * @param <ST> Related service type.
 */
public class PowerServiceImpl<ST extends PowerService & Unit> extends OpenHABService<ST> implements de.citec.dal.hal.service.PowerService {

    public PowerServiceImpl(final ST unit) throws InstantiationException {
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
