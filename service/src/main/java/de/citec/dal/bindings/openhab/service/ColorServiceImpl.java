/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.service.ColorService;
import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.unit.UnitInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rst.vision.HSVColorType;

/**
 *
 * @author mpohling
 * @param <ST> Related service type.
 */
public class ColorServiceImpl<ST extends ColorService & UnitInterface>  extends OpenHABService<ST> implements ColorService {

    public ColorServiceImpl(DeviceInterface device, ST unit) {
        super(device, unit);
    }

    @Override
    public HSVColorType.HSVColor getColor() throws CouldNotPerformException {
        return unit.getColor();
    }

    @Override
    public void setColor(HSVColorType.HSVColor color) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newHSBCommand(color));
    }
}
