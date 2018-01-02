package org.openbase.bco.manager.agent.core;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.agent.AgentClassType.AgentClass;

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
    public AgentController newInstance(final UnitConfig agentUnitConfig) throws InstantiationException {
        AgentController agent;
        try {
            if (agentUnitConfig == null) {
                throw new NotAvailableException("agentconfig");
            }
            if (!agentUnitConfig.getAgentConfig().hasAgentClassId()) {
                throw new NotAvailableException("agentype");
            }
            if (!agentUnitConfig.hasScope() && agentUnitConfig.getScope().getComponentList().isEmpty()) {
                throw new NotAvailableException("scope");
            }
            CachedAgentRegistryRemote.waitForData();
            final Class agentClass = Thread.currentThread().getContextClassLoader().loadClass(getAgentClass(agentUnitConfig));
            logger.debug("Creating agent of type [" + agentClass.getSimpleName() + "] on scope [" + ScopeGenerator.generateStringRep(agentUnitConfig.getScope()) + "]");
            agent = (AgentController) agentClass.newInstance();
            agent.init(agentUnitConfig);
        } catch (CouldNotPerformException | ClassNotFoundException | SecurityException | java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InterruptedException ex) {
            throw new InstantiationException(Agent.class, agentUnitConfig.getId(), ex);
        }
        return agent;
    }

    private String getAgentClass(final UnitConfig agentUnitConfig) throws InterruptedException, NotAvailableException {
        try {
            Registries.getAgentRegistry().waitForData();
            AgentClass agentClass = Registries.getAgentRegistry().getAgentClassById(agentUnitConfig.getAgentConfig().getAgentClassId());
            return AbstractAgentController.class.getPackage().getName() + "."
                    + "preset."
                    + agentClass.getLabel()
                    + "Agent";
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("AgentClass", ex);
        }
    }
}
