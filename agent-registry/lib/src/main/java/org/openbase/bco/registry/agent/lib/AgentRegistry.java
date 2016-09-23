package org.openbase.bco.registry.agent.lib;

/*
 * #%L
 * REM AgentRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import rst.homeautomation.control.agent.AgentClassType.AgentClass;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author mpohling
 */
public interface AgentRegistry extends Shutdownable {

    public Future<AgentConfig> registerAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public Boolean containsAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public Boolean containsAgentConfigById(String agentConfigId) throws CouldNotPerformException;

    public Future<AgentConfig> updateAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public Future<AgentConfig> removeAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public AgentConfig getAgentConfigById(final String agentConfigId) throws CouldNotPerformException;

    public List<AgentConfig> getAgentConfigs() throws CouldNotPerformException;

    public List<AgentConfig> getAgentConfigsByAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    public List<AgentConfig> getAgentConfigsByAgentClassId(String agentClassId) throws CouldNotPerformException;

    public Boolean isAgentConfigRegistryReadOnly() throws CouldNotPerformException;

    public Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    public Boolean containsAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    public Boolean containsAgentClassById(String agentClassId) throws CouldNotPerformException;

    public Future<AgentClass> updateAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    public Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    public List<AgentClass> getAgentClasses() throws CouldNotPerformException;

    public Boolean isAgentClassRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the agent class registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isAgentClassRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the agent config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isAgentConfigRegistryConsistent() throws CouldNotPerformException;

}
