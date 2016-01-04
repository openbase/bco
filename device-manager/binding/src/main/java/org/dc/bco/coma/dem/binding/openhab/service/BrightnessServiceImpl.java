/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.service;

import org.dc.bco.coma.dem.binding.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.service.BrightnessService;
import org.dc.bco.coma.dem.lib.Device;
import de.citec.dal.hal.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;

/**
 *
 * @author mpohling
 * @param <ST> Related service type.
 */
public class BrightnessServiceImpl<ST extends BrightnessService & Unit> extends OpenHABService<ST> implements BrightnessService {

    public BrightnessServiceImpl(Device device, ST unit) throws InstantiationException {
        super(device, unit);
    }

    @Override
    public Double getBrightness() throws CouldNotPerformException {
        return unit.getBrightness();
    }

    @Override
    public void setBrightness(Double brightness) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newPercentCommand(brightness));
    }
}

