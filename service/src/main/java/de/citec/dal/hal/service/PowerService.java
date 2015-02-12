/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.PowerProvider;
import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 */
public interface PowerService extends Service, PowerProvider {

    

    public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException;

}
