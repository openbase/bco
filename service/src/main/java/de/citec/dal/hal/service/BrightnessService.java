/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.BrightnessProvider;
import de.citec.jul.exception.CouldNotPerformException;

/**
 *
 * @author mpohling
 */
public interface BrightnessService extends Service, BrightnessProvider {

    public void setBrightness(Double brightness) throws CouldNotPerformException;
}
