/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core.preset;

import org.dc.bco.manager.agent.lib.Agent;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
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
    protected AgentConfig config;

    public AbstractAgent(final AgentConfig config) {
        logger.info("Creating "+getClass().getSimpleName()+"["+config.getId()+"]");
        this.config = config;
    }

    @Override
    public String getId() throws CouldNotPerformException {
        return config.getId();
    }

    @Override
    public AgentConfig getConfig() throws NotAvailableException {
        return config;
    }

    @Override
    public AbstractAgent update(AgentConfig config) throws CouldNotPerformException {
        this.config = config;
        return this;
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
