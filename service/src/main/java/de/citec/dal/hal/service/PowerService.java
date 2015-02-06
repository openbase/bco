/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 */
public interface PowerService extends Service {

    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException;

    public void setPowerState(final PowerType.Power.PowerState state) throws CouldNotPerformException;

}
