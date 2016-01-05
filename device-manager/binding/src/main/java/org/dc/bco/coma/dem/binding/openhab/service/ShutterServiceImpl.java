/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.service;

import org.dc.bco.coma.dem.binding.openhab.OpenHABCommandFactory;
import org.dc.bco.coma.dem.lib.Device;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import rst.homeautomation.state.ShutterStateType.ShutterState;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class ShutterServiceImpl<ST extends ShutterService & Unit> extends OpenHABService<ST> implements ShutterService {

    public ShutterServiceImpl(final ST unit) throws InstantiationException {
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
