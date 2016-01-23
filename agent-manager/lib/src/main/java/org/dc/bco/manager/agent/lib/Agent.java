/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.lib;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.iface.Identifiable;
import rst.homeautomation.state.ActivationStateType;

/**
 *
 * @author mpohling
 */
public interface Agent extends Identifiable<String> {

    public void setActivationState(ActivationStateType.ActivationState activation) throws CouldNotPerformException;
}
