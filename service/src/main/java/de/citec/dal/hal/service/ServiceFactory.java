/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.AbstractDeviceController;

/**
 *
 * @author mpohling
 */
public interface ServiceFactory {

    public abstract BrightnessService newBrightnessService(AbstractDeviceController device, BrightnessService unit);

    public abstract ColorService newColorService(AbstractDeviceController device, ColorService unit);

    public abstract PowerService newPowerService(AbstractDeviceController device, PowerService unit);
    
}
