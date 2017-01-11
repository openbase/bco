package org.openbase.bco.manager.agent.core;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.manager.agent.lib.Agent;
import org.openbase.bco.manager.agent.lib.AgentController;
import org.openbase.bco.manager.agent.lib.AgentFactory;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
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
    public AgentController newInstance(final UnitConfig config) throws InstantiationException {
        AgentController agent;
        try {
            if (config == null) {
                throw new NotAvailableException("agentconfig");
            }
            if (!config.getAgentConfig().hasAgentClassId()) {
                throw new NotAvailableException("agentype");
            }
            if (!config.hasScope() && config.getScope().getComponentList().isEmpty()) {
                throw new NotAvailableException("scope");
            }
            CachedAgentRegistryRemote.waitForData();
            String agentClassLabel = CachedAgentRegistryRemote.getRegistry().getAgentClassById(config.getAgentConfig().getAgentClassId()).getLabel();
            final Class agentClass = Thread.currentThread().getContextClassLoader().loadClass(getAgentClass(agentClassLabel));
            logger.info("Creating agent of type [" + agentClass.getSimpleName() + "] on scope [" + ScopeGenerator.generateStringRep(config.getScope()) + "]");
            agent = (AgentController) agentClass.newInstance();
            agent.init(config);
        } catch (CouldNotPerformException | ClassNotFoundException | SecurityException | java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InterruptedException ex) {
            throw new InstantiationException(Agent.class, config.getId(), ex);
        }
        return agent;
    }

    private String getAgentClass(final String agentClassLabel) {
        return AbstractAgent.class.getPackage().getName() + "."
                + "preset."
                + agentClassLabel
                + "Agent";
    }
}
