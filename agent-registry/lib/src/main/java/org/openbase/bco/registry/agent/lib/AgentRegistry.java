package org.openbase.bco.registry.agent.lib;

/*
 * #%L
 * BCO Registry Agent Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.registry.AgentRegistryDataType.AgentRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.agent.AgentClassType.AgentClass;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface AgentRegistry extends DataProvider<AgentRegistryData>, Shutdownable {

    @RPCMethod
    public Future<UnitConfig> registerAgentConfig(UnitConfig agentUnitConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsAgentConfig(UnitConfig agentUnitConfig) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsAgentConfigById(String agentUnitConfigId) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> updateAgentConfig(UnitConfig agentUnitConfig) throws CouldNotPerformException;

    @RPCMethod
    public Future<UnitConfig> removeAgentConfig(UnitConfig agentUnitConfig) throws CouldNotPerformException;

    @RPCMethod
    public UnitConfig getAgentConfigById(final String agentUnitConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getAgentConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getAgentConfigsByAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    public List<UnitConfig> getAgentConfigsByAgentClassId(String agentClassId) throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAgentConfigRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsAgentClassById(String agentClassId) throws CouldNotPerformException;

    @RPCMethod
    public Future<AgentClass> updateAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    @RPCMethod
    public Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    public List<AgentClass> getAgentClasses() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isAgentClassRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public AgentClass getAgentClassById(final String agentClassId) throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the agent class registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isAgentClassRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the agent config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isAgentConfigRegistryConsistent() throws CouldNotPerformException;

}
