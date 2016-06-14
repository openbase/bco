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
import org.openbase.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author mpohling
 */
public interface AgentRegistry {

    public Future<AgentConfig> registerAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public Boolean containsAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public Boolean containsAgentConfigById(String agentConfigId) throws CouldNotPerformException;

    public Future<AgentConfig> updateAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public Future<AgentConfig> removeAgentConfig(AgentConfig agentConfig) throws CouldNotPerformException;

    public AgentConfig getAgentConfigById(final String agentConfigId) throws CouldNotPerformException;

    public List<AgentConfig> getAgentConfigs() throws CouldNotPerformException;

    public Boolean isAgentConfigRegistryReadOnly() throws CouldNotPerformException;

    public void shutdown();
}
