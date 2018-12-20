package org.openbase.bco.registry.clazz.lib;

/*-
 * #%L
 * BCO Registry Class Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.registry.lib.provider.clazz.AgentClassCollectionProvider;
import org.openbase.bco.registry.lib.provider.clazz.AppClassCollectionProvider;
import org.openbase.bco.registry.lib.provider.clazz.DeviceClassCollectionProvider;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;
import org.openbase.type.domotic.registry.ClassRegistryDataType.ClassRegistryData;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.concurrent.Future;

public interface ClassRegistry extends AppClassCollectionProvider, AgentClassCollectionProvider, DeviceClassCollectionProvider, DataProvider<ClassRegistryData>, Shutdownable {

    // ===================================== DeviceClass Methods =============================================================

    /**
     * Method registers the given device class.
     *
     * @param deviceClass the device class to be registered.
     * @return the registered device class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    /**
     * Method registers a device class encoded in a transaction value.
     *
     * @param transactionValue the device class to be registered encoded in a transaction value
     * @return a transaction value containing the registered device class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> registerDeviceClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method updates the given device class.
     *
     * @param deviceClass the updated device class.
     * @return the updated device class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    /**
     * Method updates a device class encoded in a transaction value.
     *
     * @param transactionValue the device class to be updated encoded in a transaction value
     * @return a transaction value containing the updated device class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> updateDeviceClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method removes the given device class.
     *
     * @param deviceClass the device class to be removed.
     * @return the removed device class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    /**
     * Method removed a device class encoded in a transaction value.
     *
     * @param transactionValue the device class to be removed encoded in a transaction value
     * @return a transaction value containing the removed device class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> removeDeviceClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as read only.
     *
     * @return if the device class registry is read only
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the device class registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException;

    // ===================================== AgentClass Methods =============================================================

    /**
     * Method registers the given agent class.
     *
     * @param agentClass the agent class to be registered.
     * @return the registered agent class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    /**
     * Method registers an agent class encoded in a transaction value.
     *
     * @param transactionValue the agent class to be registered encoded in a transaction value
     * @return a transaction value containing the registered agent class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> registerAgentClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method updates the given agent class.
     *
     * @param agentClass the updated agent class.
     * @return the updated agent class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<AgentClass> updateAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    /**
     * Method updates an agent class encoded in a transaction value.
     *
     * @param transactionValue the agent class to be updated encoded in a transaction value
     * @return a transaction value containing the updated agent class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> updateAgentClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method removes the given agent class.
     *
     * @param agentClass the agent class to be removed.
     * @return the removed agent class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    /**
     * Method removes an agent class encoded in a transaction value.
     *
     * @param transactionValue the agent class to be removed encoded in a transaction value
     * @return a transaction value containing the removed agent class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> removeAgentClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as read only.
     *
     * @return if the agent class registry is read only
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAgentClassRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the agent class registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAgentClassRegistryConsistent() throws CouldNotPerformException;

    // ===================================== AppClass Methods =============================================================

    /**
     * Method registers the given app class.
     *
     * @param appClass the app class to be registered.
     * @return the registered app class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<AppClass> registerAppClass(AppClass appClass) throws CouldNotPerformException;

    /**
     * Method registers an app class encoded in a transaction value.
     *
     * @param transactionValue the app class to be registered encoded in a transaction value
     * @return a transaction value containing the registered app class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> registerAppClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method updates the given app class.
     *
     * @param appClass the updated app class.
     * @return the updated app class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<AppClass> updateAppClass(AppClass appClass) throws CouldNotPerformException;

    /**
     * Method updates an app class encoded in a transaction value.
     *
     * @param transactionValue the app class to be updated encoded in a transaction value
     * @return a transaction value containing the updated app class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> updateAppClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method removes the given app class.
     *
     * @param appClass the app class to be removed.
     * @return the removed app class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<AppClass> removeAppClass(AppClass appClass) throws CouldNotPerformException;

    /**
     * Method removes an app class encoded in a transaction value.
     *
     * @param transactionValue the app class to be removed encoded in a transaction value
     * @return a transaction value containing the removed app class and an id for this transaction
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> removeAppClassVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as read only.
     *
     * @return if the app class registry is read only
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAppClassRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the app class registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAppClassRegistryConsistent() throws CouldNotPerformException;

}
