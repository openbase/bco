/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core;

import java.lang.reflect.InvocationTargetException;
import org.dc.bco.manager.agent.lib.Agent;
import org.dc.bco.manager.agent.core.preset.AbstractAgent;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.InstantiationException;
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
    public Agent newInstance(final AgentConfig config) throws InstantiationException {
        try {
            if (config == null) {
                throw new NotAvailableException("agentconfig");
            }
            if (!config.hasType()) {
                throw new NotAvailableException("agentype");
            }
            final Class agentClass = Thread.currentThread().getContextClassLoader().loadClass(getAgentClass(config));
            logger.info("Creating agent of type [" + agentClass.getSimpleName() + "]");
            return (Agent) agentClass.getConstructor(AgentConfig.class).newInstance(config);
        } catch (NotAvailableException | ClassNotFoundException | NoSuchMethodException | SecurityException | java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new InstantiationException(Agent.class, config.getId(), ex);
        }
    }

    private String getAgentClass(final AgentConfigType.AgentConfig config) {
        return AbstractAgent.class.getPackage().getName() + "."
                + StringProcessor.transformUpperCaseToCamelCase(config.getType().name())
                + "Agent";
    }
}
