/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.service.BrightnessService;
import de.citec.dal.hal.service.ColorService;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;

/**
 *
 * @author mpohling
 */
public class OpenhabServiceFactory implements ServiceFactory {

    
    
    public BrightnessService newBrightnessService(AbstractDeviceController device, BrightnessService unit) {
          return new BrightnessServiceImpl(device, unit);
    }

    @Override
    public ColorService newColorService(AbstractDeviceController device, ColorService unit) {
        return new ColorServiceImpl(device, unit);
    }

    @Override
    public PowerService newPowerService(AbstractDeviceController device, PowerService unit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

     
}
