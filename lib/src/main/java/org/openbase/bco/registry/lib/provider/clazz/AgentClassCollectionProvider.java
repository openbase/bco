package org.openbase.bco.registry.lib.provider.clazz;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;

import java.util.List;

public interface AgentClassCollectionProvider {

    /**
     * Method returns true if a agent class with the given id is
     * registered, otherwise false.
     *
     * @param agentClassId the id of the agent class
     * @return true if a agent class with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsAgentClassById(String agentClassId) throws CouldNotPerformException;

    /**
     * Method returns true if the agent class with the given id is
     * registered, otherwise false. The agent class id field is used for the
     * comparison.
     *
     * @param agentClass the agent class which is tested
     * @return true if a agent class with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    /**
     * Method returns all registered agent classes.
     *
     * @return the agent classes stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<AgentClass> getAgentClasses() throws CouldNotPerformException;

    /**
     * Method returns the agent class which is registered with the given
     * id.
     *
     * @param agentClassId the id of the agent class
     * @return the requested agent class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    AgentClass getAgentClassById(final String agentClassId) throws CouldNotPerformException;
}
