/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.agm.core;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.pattern.Factory;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface AgentFactory extends Factory<Agent, AgentConfig> {

    @Override
    public Agent newInstance(final AgentConfig config) throws CouldNotPerformException;

}
