/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.ShutterStateProvider;
import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.states.ShutterType;

/**
 *
 * @author thuxohl
 */
public interface ShutterService extends Service, ShutterStateProvider {
    
    public void setShutterState(ShutterType.Shutter.ShutterState state) throws CouldNotPerformException;
}
