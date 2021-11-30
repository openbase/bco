package org.openbase.bco.registry.clazz.remote;

/*
 * #%L
 * BCO Registry Class Remote
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

import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.clazz.lib.jp.JPClassRegistryScope;
import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.RegistryVerifiedCommunicationHelper;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.communication.controller.RPCUtils;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.util.TransactionSynchronizationFuture;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;
import org.openbase.type.domotic.registry.ClassRegistryDataType.ClassRegistryData;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ClassRegistryRemote extends AbstractRegistryRemote<ClassRegistryData> implements ClassRegistry, RegistryRemote<ClassRegistryData>, DataProvider<ClassRegistryData> {

    private final SynchronizedRemoteRegistry<String, AgentClass, AgentClass.Builder> agentClassRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, AppClass, AppClass.Builder> appClassRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, DeviceClass, DeviceClass.Builder> deviceClassRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, GatewayClass, GatewayClass.Builder> gatewayClassRemoteRegistry;

    public ClassRegistryRemote() throws InstantiationException {
        super(JPClassRegistryScope.class, ClassRegistryData.class);

        try {
            agentClassRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, ClassRegistryData.AGENT_CLASS_FIELD_NUMBER);
            appClassRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, ClassRegistryData.APP_CLASS_FIELD_NUMBER);
            deviceClassRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, ClassRegistryData.DEVICE_CLASS_FIELD_NUMBER);
            gatewayClassRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, ClassRegistryData.GATEWAY_CLASS_FIELD_NUMBER);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @throws InterruptedException     {@inheritDoc }
     * @throws CouldNotPerformException {@inheritDoc }
     */
    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (!CachedClassRegistryRemote.getRegistry().equals(this)) {
            logger.warn("You are using a " + getClass().getSimpleName() + " which is not maintained by the global registry singelton! This is extremely inefficient! Please use \"Registries.get" + getClass().getSimpleName().replace("Remote", "") + "()\" instead creating your own instances!");
        }
        super.activate();
    }

    @Override
    protected void registerRemoteRegistries() {
        registerRemoteRegistry(agentClassRemoteRegistry);
        registerRemoteRegistry(appClassRemoteRegistry);
        registerRemoteRegistry(deviceClassRemoteRegistry);
        registerRemoteRegistry(gatewayClassRemoteRegistry);
    }

    /**
     * Get the internally used agent class remote registry.
     *
     * @return the internally used agent class remote registry
     */
    public SynchronizedRemoteRegistry<String, AgentClass, AgentClass.Builder> getAgentClassRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("AgentClassRemoteRegistry", ex);
            }
        }
        return agentClassRemoteRegistry;
    }

    /**
     * Get the internally used app class remote registry.
     *
     * @return the internally used app class remote registry
     */
    public SynchronizedRemoteRegistry<String, AppClass, AppClass.Builder> getAppClassRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("AppClassRemoteRegistry", ex);
            }
        }
        return appClassRemoteRegistry;
    }

    /**
     * Get the internally used device class remote registry.
     *
     * @return the internally used device class remote registry
     */
    public SynchronizedRemoteRegistry<String, DeviceClass, DeviceClass.Builder> getDeviceClassRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("DeviceClassRemoteRegistry", ex);
            }
        }
        return deviceClassRemoteRegistry;
    }

    /**
     * Get the internally used gateway class remote registry.
     *
     * @return the internally used gateway class remote registry
     */
    public SynchronizedRemoteRegistry<String, GatewayClass, GatewayClass.Builder> getGatewayClassRemoteRegistry(final boolean validateConnection) throws NotAvailableException {
        if (validateConnection) {
            try {
                validateData();
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("GatewayClassRemoteRegistry", ex);
            }
        }
        return gatewayClassRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isConsistent() {
        return isDeviceClassRegistryConsistent()
                && isAgentClassRegistryConsistent()
                && isAppClassRegistryConsistent()
                && isGatewayClassRegistryConsistent();
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        CachedTemplateRegistryRemote.getRegistry().waitForData();
        super.waitForData();
    }

    // --------------------------------------------- device ------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException    {@inheritDoc}
     */
    @Override
    public DeviceClass getDeviceClassById(String deviceClassId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return deviceClassRemoteRegistry.getMessage(deviceClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(deviceClass, transactionValue -> registerDeviceClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> registerDeviceClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceClass(final DeviceClass deviceClass) {
        try {
            validateData();
            return deviceClassRemoteRegistry.contains(deviceClass);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceClassById(String deviceClassId) {
        try {
            validateData();
            return deviceClassRemoteRegistry.contains(deviceClassId);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(deviceClass, transactionValue -> updateDeviceClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> updateDeviceClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(deviceClass, transactionValue -> removeDeviceClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> removeDeviceClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException    {@inheritDoc}
     */
    @Override
    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException, NotAvailableException {
        validateData();
        return deviceClassRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isDeviceClassRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getDeviceClassRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isDeviceClassRegistryConsistent() {
        try {
            validateData();
            return getData().getDeviceClassRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    // --------------------------------------------- agent -------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param agentClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AgentClass> registerAgentClass(AgentClass agentClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(agentClass, transactionValue -> registerAgentClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> registerAgentClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param agentClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsAgentClass(AgentClass agentClass) {
        try {
            validateData();
            return agentClassRemoteRegistry.contains(agentClass);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param agentClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsAgentClassById(String agentClassId) {
        try {
            validateData();
            return agentClassRemoteRegistry.contains(agentClassId);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param agentClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AgentClass> updateAgentClass(AgentClass agentClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(agentClass, transactionValue -> updateAgentClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> updateAgentClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param agentClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AgentClass> removeAgentClass(AgentClass agentClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(agentClass, transactionValue -> removeAgentClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> removeAgentClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<AgentClass> getAgentClasses() throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isAgentClassRegistryReadOnly() {
        try {
            validateData();
            return getData().getAgentClassRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param agentClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public AgentClass getAgentClassById(String agentClassId) throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.getMessage(agentClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isAgentClassRegistryConsistent() {
        try {
            validateData();
            return getData().getAgentClassRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }


    // --------------------------------------------- app ---------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param appClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AppClass> registerAppClass(AppClass appClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(appClass, transactionValue -> registerAppClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> registerAppClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param appClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsAppClass(AppClass appClass) {
        try {
            validateData();
            return appClassRemoteRegistry.contains(appClass);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param appClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsAppClassById(String appClassId) {
        try {
            validateData();
            return appClassRemoteRegistry.contains(appClassId);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param appClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AppClass> updateAppClass(AppClass appClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(appClass, transactionValue -> updateAppClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> updateAppClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param appClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AppClass> removeAppClass(AppClass appClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(appClass, transactionValue -> removeAppClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> removeAppClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param appClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public AppClass getAppClassById(String appClassId) throws CouldNotPerformException {
        validateData();
        return appClassRemoteRegistry.getMessage(appClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<AppClass> getAppClasses() throws CouldNotPerformException {
        validateData();
        return appClassRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isAppClassRegistryReadOnly() {
        try {
            validateData();
            return getData().getAppClassRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isAppClassRegistryConsistent() {
        try {
            validateData();
            return getData().getAppClassRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    // --------------------------------------------- gateway ------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param gatewayClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException    {@inheritDoc}
     */
    @Override
    public GatewayClass getGatewayClassById(String gatewayClassId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return gatewayClassRemoteRegistry.getMessage(gatewayClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<GatewayClass> registerGatewayClass(final GatewayClass gatewayClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(gatewayClass, transactionValue -> registerGatewayClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> registerGatewayClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsGatewayClass(final GatewayClass gatewayClass) {
        try {
            validateData();
            return gatewayClassRemoteRegistry.contains(gatewayClass);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsGatewayClassById(String gatewayClassId) {
        try {
            validateData();
            return gatewayClassRemoteRegistry.contains(gatewayClassId);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<GatewayClass> updateGatewayClass(final GatewayClass gatewayClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(gatewayClass, transactionValue -> updateGatewayClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> updateGatewayClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<GatewayClass> removeGatewayClass(final GatewayClass gatewayClass) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(gatewayClass, transactionValue -> removeGatewayClassVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> removeGatewayClassVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCUtils.callRemoteServerMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws NotAvailableException    {@inheritDoc}
     */
    @Override
    public List<GatewayClass> getGatewayClasses() throws CouldNotPerformException, NotAvailableException {
        validateData();
        return gatewayClassRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isGatewayClassRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getGatewayClassRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isGatewayClassRegistryConsistent() {
        try {
            validateData();
            return getData().getGatewayClassRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }
}
