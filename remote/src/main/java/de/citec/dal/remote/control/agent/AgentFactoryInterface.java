/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent;

import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.control.agent.AgentConfigType;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface AgentFactoryInterface {

    AgentInterface newAgent(final AgentConfigType.AgentConfig config) throws CouldNotPerformException;

}
