/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.bindings.openhab.OpenHABServiceImpl;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.service.ColorService;
import de.citec.jul.exception.CouldNotPerformException;
import rst.vision.HSVColorType;

/**
 *
 * @author mpohling
 */
public class ColorServiceImpl extends OpenHABServiceImpl<ColorService> implements ColorService {

    public ColorServiceImpl(AbstractDeviceController device, ColorService unit) {
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
