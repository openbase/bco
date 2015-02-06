/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.hal.service.BrightnessService;
import de.citec.dal.hal.service.ColorService;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.dal.hal.unit.DeviceInterface;

/**
 *
 * @author mpohling
 */
public class OpenhabServiceFactory implements ServiceFactory {

    @Override
    public BrightnessService newBrightnessService(DeviceInterface device, BrightnessService unit) {
        return new BrightnessServiceImpl(device, unit);
    }

    @Override
    public ColorService newColorService(DeviceInterface device, ColorService unit) {
        return new ColorServiceImpl(device, unit);
    }

    @Override
    public PowerService newPowerService(DeviceInterface device, PowerService unit) {
        return new PowerServiceImpl(device, unit);
    }
}
