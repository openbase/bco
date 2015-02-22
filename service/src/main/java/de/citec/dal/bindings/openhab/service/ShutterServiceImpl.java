/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.service.ShutterService;
import de.citec.dal.hal.unit.UnitInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.ShutterType;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class ShutterServiceImpl<ST extends ShutterService & UnitInterface> extends OpenHABService<ST> implements ShutterService {

    public ShutterServiceImpl(DeviceInterface device, ST unit) {
        super(device, unit);
    }

    @Override
    public void setShutter(ShutterType.Shutter.ShutterState state) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newUpDownCommand(state));
    }

    @Override
    public ShutterType.Shutter.ShutterState getShutter() throws CouldNotPerformException {
        return unit.getShutter();
    }

}
