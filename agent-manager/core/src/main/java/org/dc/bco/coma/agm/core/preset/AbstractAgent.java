/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.agm.core.preset;

import org.dc.bco.coma.agm.lib.Agent;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public abstract class AbstractAgent implements Agent {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean active;
    protected final AgentConfig agentConfig;

    public AbstractAgent(final AgentConfig agentConfig) {
        logger.info("Creating "+getClass().getSimpleName()+"["+agentConfig.getId()+"]");
        this.agentConfig = agentConfig;
    }

    @Override
    public String getId() throws CouldNotPerformException {
        return agentConfig.getId();
    }

    @Override
    public AgentConfig getConfig() throws NotAvailableException {
        return agentConfig;
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
