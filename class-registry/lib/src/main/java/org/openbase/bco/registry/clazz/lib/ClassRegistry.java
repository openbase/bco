package org.openbase.bco.registry.clazz.lib;

/*-
 * #%L
 * BCO Registry Class Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.registry.lib.provider.clazz.GatewayClassCollectionProvider;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;
import org.openbase.type.domotic.registry.ClassRegistryDataType.ClassRegistryData;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;

import java.util.concurrent.Future;

public interface ClassRegistry extends AppClassCollectionProvider, AgentClassCollectionProvider, DeviceClassCollectionProvider, GatewayClassCollectionProvider, DataProvider<ClassRegistryData>, Shutdownable {

    // ===================================== DeviceClass Methods =============================================================

    /**
     * Method registers the given device class.
     *
     * @param deviceClass the device class to be registered.
     *
     * @return the registered device class.
     */
    @RPCMethod
    Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass);

    /**
     * Method registers a device class encoded in a transaction value.
     *
     * @param transactionValue the device class to be registered encoded in a transaction value
     *
     * @return a transaction value containing the registered device class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> registerDeviceClassVerified(final TransactionValue transactionValue);

    /**
     * Method updates the given device class.
     *
     * @param deviceClass the updated device class.
     *
     * @return the updated device class.
     */
    @RPCMethod
    Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass);

    /**
     * Method updates a device class encoded in a transaction value.
     *
     * @param transactionValue the device class to be updated encoded in a transaction value
     *
     * @return a transaction value containing the updated device class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> updateDeviceClassVerified(final TransactionValue transactionValue);

    /**
     * Method removes the given device class.
     *
     * @param deviceClass the device class to be removed.
     *
     * @return the removed device class.
     */
    @RPCMethod
    Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass);

    /**
     * Method removed a device class encoded in a transaction value.
     *
     * @param transactionValue the device class to be removed encoded in a transaction value
     *
     * @return a transaction value containing the removed device class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> removeDeviceClassVerified(final TransactionValue transactionValue);

    /**
     * Method returns true if the underlying registry is marked as read only.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the device class registry is read only
     */
    @RPCMethod
    Boolean isDeviceClassRegistryReadOnly();

    /**
     * Method returns true if the underlying registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the device class registry is consistent
     */
    @RPCMethod
    Boolean isDeviceClassRegistryConsistent();

    // ===================================== AgentClass Methods =============================================================

    /**
     * Method registers the given agent class.
     *
     * @param agentClass the agent class to be registered.
     *
     * @return the registered agent class.
     */
    @RPCMethod
    Future<AgentClass> registerAgentClass(AgentClass agentClass);

    /**
     * Method registers an agent class encoded in a transaction value.
     *
     * @param transactionValue the agent class to be registered encoded in a transaction value
     *
     * @return a transaction value containing the registered agent class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> registerAgentClassVerified(final TransactionValue transactionValue);

    /**
     * Method updates the given agent class.
     *
     * @param agentClass the updated agent class.
     *
     * @return the updated agent class.
     */
    @RPCMethod
    Future<AgentClass> updateAgentClass(AgentClass agentClass);

    /**
     * Method updates an agent class encoded in a transaction value.
     *
     * @param transactionValue the agent class to be updated encoded in a transaction value
     *
     * @return a transaction value containing the updated agent class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> updateAgentClassVerified(final TransactionValue transactionValue);

    /**
     * Method removes the given agent class.
     *
     * @param agentClass the agent class to be removed.
     *
     * @return the removed agent class.
     */
    @RPCMethod
    Future<AgentClass> removeAgentClass(AgentClass agentClass);

    /**
     * Method removes an agent class encoded in a transaction value.
     *
     * @param transactionValue the agent class to be removed encoded in a transaction value
     *
     * @return a transaction value containing the removed agent class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> removeAgentClassVerified(final TransactionValue transactionValue);

    /**
     * Method returns true if the underlying registry is marked as read only.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the agent class registry is read only
     */
    @RPCMethod
    Boolean isAgentClassRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the agent class registry is consistent
     */
    @RPCMethod
    Boolean isAgentClassRegistryConsistent();

    // ===================================== AppClass Methods =============================================================

    /**
     * Method registers the given app class.
     *
     * @param appClass the app class to be registered.
     *
     * @return the registered app class.
     */
    @RPCMethod
    Future<AppClass> registerAppClass(AppClass appClass);

    /**
     * Method registers an app class encoded in a transaction value.
     *
     * @param transactionValue the app class to be registered encoded in a transaction value
     *
     * @return a transaction value containing the registered app class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> registerAppClassVerified(final TransactionValue transactionValue);

    /**
     * Method updates the given app class.
     *
     * @param appClass the updated app class.
     *
     * @return the updated app class.
     */
    @RPCMethod
    Future<AppClass> updateAppClass(AppClass appClass);

    /**
     * Method updates an app class encoded in a transaction value.
     *
     * @param transactionValue the app class to be updated encoded in a transaction value
     *
     * @return a transaction value containing the updated app class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> updateAppClassVerified(final TransactionValue transactionValue);

    /**
     * Method removes the given app class.
     *
     * @param appClass the app class to be removed.
     *
     * @return the removed app class.
     */
    @RPCMethod
    Future<AppClass> removeAppClass(AppClass appClass);

    /**
     * Method removes an app class encoded in a transaction value.
     *
     * @param transactionValue the app class to be removed encoded in a transaction value
     *
     * @return a transaction value containing the removed app class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> removeAppClassVerified(final TransactionValue transactionValue);

    /**
     * Method returns true if the underlying registry is marked as read only.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the app class registry is read only
     */
    @RPCMethod
    Boolean isAppClassRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the app class registry is consistent
     */
    @RPCMethod
    Boolean isAppClassRegistryConsistent();

    // ===================================== GatewayClass Methods =============================================================

    /**
     * Method registers the given gateway class.
     *
     * @param gatewayClass the gateway class to be registered.
     *
     * @return the registered gateway class.
     */
    @RPCMethod
    Future<GatewayClass> registerGatewayClass(GatewayClass gatewayClass);

    /**
     * Method registers an gateway class encoded in a transaction value.
     *
     * @param transactionValue the gateway class to be registered encoded in a transaction value
     *
     * @return a transaction value containing the registered gateway class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> registerGatewayClassVerified(final TransactionValue transactionValue);

    /**
     * Method updates the given gateway class.
     *
     * @param gatewayClass the updated gateway class.
     *
     * @return the updated gateway class.
     */
    @RPCMethod
    Future<GatewayClass> updateGatewayClass(GatewayClass gatewayClass);

    /**
     * Method updates an gateway class encoded in a transaction value.
     *
     * @param transactionValue the gateway class to be updated encoded in a transaction value
     *
     * @return a transaction value containing the updated gateway class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> updateGatewayClassVerified(final TransactionValue transactionValue);

    /**
     * Method removes the given gateway class.
     *
     * @param gatewayClass the gateway class to be removed.
     *
     * @return the removed gateway class.
     */
    @RPCMethod
    Future<GatewayClass> removeGatewayClass(GatewayClass gatewayClass);

    /**
     * Method removes an gateway class encoded in a transaction value.
     *
     * @param transactionValue the gateway class to be removed encoded in a transaction value
     *
     * @return a transaction value containing the removed gateway class and an id for this transaction
     */
    @RPCMethod
    Future<TransactionValue> removeGatewayClassVerified(final TransactionValue transactionValue);

    /**
     * Method returns true if the underlying registry is marked as read only.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the gateway class registry is read only
     */
    @RPCMethod
    Boolean isGatewayClassRegistryReadOnly();

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the gateway class registry is consistent
     */
    @RPCMethod
    Boolean isGatewayClassRegistryConsistent();

}
