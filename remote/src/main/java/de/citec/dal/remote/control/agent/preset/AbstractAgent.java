/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent.preset;

import de.citec.dal.remote.control.agent.Agent;
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

    public AbstractAgent(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
        logger.info("Agent[" + getClass().getSimpleName() + "] state chance to "+agentConfig.getActivationState().getValue());
        switch (agentConfig.getActivationState().getValue()) {
            case ACTIVE:
                active = true;
                break;
            case DEACTIVE:
                active = false;
                break;
            case UNKNOWN:
            default:
                logger.warn("Agent [" + getClass().getSimpleName() + "] activation state is unknown!");
        }
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
