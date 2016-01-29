/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.lib;

import org.dc.jul.exception.InstantiationException;
import org.dc.jul.pattern.Factory;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface AgentFactory extends Factory<AgentController, AgentConfig> {

    @Override
    public AgentController newInstance(final AgentConfig config) throws InstantiationException;

}
