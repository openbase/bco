/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.ShutterService;
import de.citec.dal.hal.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import rst.homeautomation.state.ShutterStateType.ShutterState;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class ShutterServiceImpl<ST extends ShutterService & Unit> extends OpenHABService<ST> implements ShutterService {

    public ShutterServiceImpl(Device device, ST unit) throws InstantiationException {
        super(device, unit);
    }

    @Override
    public void setShutter(ShutterState.State state) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newUpDownCommand(state));
    }

    @Override
    public ShutterState getShutter() throws CouldNotPerformException {
        return unit.getShutter();
    }

}
