/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.service;

import org.dc.bco.coma.dem.binding.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.service.ColorService;
import org.dc.bco.coma.dem.lib.Device;
import de.citec.dal.hal.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import rst.vision.HSVColorType;

/**
 *
 * @author mpohling
 * @param <ST> Related service type.
 */
public class ColorServiceImpl<ST extends ColorService & Unit>  extends OpenHABService<ST> implements ColorService {

    public ColorServiceImpl(final ST unit) throws InstantiationException {
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
