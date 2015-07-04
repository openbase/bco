/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import rst.homeautomation.control.agent.AgentConfigType;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class Agent implements AgentInterface  {

    private AgentConfigType.AgentConfig config;

    public Agent(AgentConfigType.AgentConfig config) {
        this.config = config;
        System.out.println("create agent:" + config.getLabel());
    }

    @Override
    public AgentConfigType.AgentConfig getConfig() throws NotAvailableException {
        return config;
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        System.out.println("activate agent:" + config.getLabel());
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        System.out.println("deactivate agent:" + config.getLabel());
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
