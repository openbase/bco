/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core;

import java.util.logging.Level;
import org.dc.bco.manager.agent.lib.Agent;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.processing.StringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class AgentFactoryImpl implements AgentFactory {

    protected final Logger logger = LoggerFactory.getLogger(AgentFactoryImpl.class);
    private static AgentFactoryImpl instance;

    public synchronized static AgentFactory getInstance() {

        if (instance == null) {
            instance = new AgentFactoryImpl();
        }
        return instance;
    }

    private AgentFactoryImpl() {

    }

    @Override
    public AgentController newInstance(final AgentConfig config) throws InstantiationException {
        AgentController agent;
        try {
            if (config == null) {
                throw new NotAvailableException("agentconfig");
            }
            if (!config.hasType()) {
                throw new NotAvailableException("agentype");
            }
            final Class agentClass = Thread.currentThread().getContextClassLoader().loadClass(getAgentClass(config));
            logger.info("Creating agent of type [" + agentClass.getSimpleName() + "]");
            agent = (AgentController) agentClass.newInstance();
            agent.init(config);
        } catch (CouldNotPerformException | ClassNotFoundException | SecurityException | java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InterruptedException ex) {
            throw new InstantiationException(Agent.class, config.getId(), ex);
        }
        return agent;
    }

    private String getAgentClass(final AgentConfigType.AgentConfig config) {
        return AbstractAgent.class.getPackage().getName() + "."
                + "preset."
                + StringProcessor.transformUpperCaseToCamelCase(config.getType().name())
                + "Agent";
    }
}
