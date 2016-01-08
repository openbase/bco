/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.service;

import org.dc.bco.manager.device.binding.openhab.execution.OpenHABCommandFactory;
import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;

/**
 *
 * @author mpohling
 * @param <UNIT> Related unit.
 */
public class BrightnessServiceImpl<UNIT extends BrightnessService & Unit> extends OpenHABService<UNIT> implements BrightnessService {

    public BrightnessServiceImpl(final UNIT unit) throws InstantiationException {
        super(unit);
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

