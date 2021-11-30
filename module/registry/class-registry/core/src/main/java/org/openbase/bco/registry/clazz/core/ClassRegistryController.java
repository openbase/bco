package org.openbase.bco.registry.clazz.core;

import org.openbase.bco.registry.clazz.core.consistency.*;
import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.clazz.lib.jp.*;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.lib.com.RegistryVerifiedCommunicationHelper;
import org.openbase.bco.registry.lib.generator.UUIDGenerator;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.communication.iface.RPCServer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;
import org.openbase.type.domotic.registry.ClassRegistryDataType.ClassRegistryData;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.List;
import java.util.concurrent.Future;

/*
 * #%L
 * BCO Registry Class Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ClassRegistryController extends AbstractRegistryController<ClassRegistryData, ClassRegistryData.Builder> implements ClassRegistry, DataProvider<ClassRegistryData>, Shutdownable {

    private ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, ClassRegistryData.Builder> agentClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, ClassRegistryData.Builder> appClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, ClassRegistryData.Builder> deviceClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, GatewayClass, GatewayClass.Builder, ClassRegistryData.Builder> gatewayClassRegistry;

    public ClassRegistryController() throws InstantiationException, InterruptedException {
        super(JPClassRegistryScope.class, ClassRegistryData.newBuilder(), SPARSELY_REGISTRY_DATA_NOTIFIED);
        try {
            agentClassRegistry = new ProtoBufFileSynchronizedRegistry<>(AgentClass.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(ClassRegistryData.AGENT_CLASS_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPAgentClassDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider,
                    false);

            appClassRegistry = new ProtoBufFileSynchronizedRegistry<>(AppClass.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(ClassRegistryData.APP_CLASS_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPAppClassDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider,
                    false);

            deviceClassRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceClass.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(ClassRegistryData.DEVICE_CLASS_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider,
                    false);

            gatewayClassRegistry = new ProtoBufFileSynchronizedRegistry<>(GatewayClass.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(ClassRegistryData.GATEWAY_CLASS_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPGatewayClassDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider,
                    false);
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        deviceClassRegistry.registerConsistencyHandler(new DeviceClassUnitTemplateConfigConsistencyHandler());
        deviceClassRegistry.registerConsistencyHandler(new DeviceClassRequiredFieldConsistencyHandler());
        gatewayClassRegistry.registerConsistencyHandler(new GatewayClassRequiredFieldConsistencyHandler());
        gatewayClassRegistry.registerConsistencyHandler(new GatewayClassNestedGatewayConsistencyHandler());
        deviceClassRegistry.registerConsistencyHandler(new KNXDeviceClassConsistencyHandler());
    }

    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        agentClassRegistry.registerDependency(CachedTemplateRegistryRemote.getRegistry().getUnitTemplateRemoteRegistry(false));
        appClassRegistry.registerDependency(CachedTemplateRegistryRemote.getRegistry().getUnitTemplateRemoteRegistry(false));
        deviceClassRegistry.registerDependency(CachedTemplateRegistryRemote.getRegistry().getUnitTemplateRemoteRegistry(false));
        gatewayClassRegistry.registerDependency(CachedTemplateRegistryRemote.getRegistry().getUnitTemplateRemoteRegistry(false));
    }

    @Override
    protected void registerPlugins() {
    }

    @Override
    protected void registerRegistries() {
        registerRegistry(agentClassRegistry);
        registerRegistry(appClassRegistry);
        registerRegistry(deviceClassRegistry);
        registerRegistry(gatewayClassRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException       {@inheritDoc}
     */
    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException {
        setDataField(ClassRegistryData.AGENT_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, agentClassRegistry.isReadOnly());
        setDataField(ClassRegistryData.AGENT_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, agentClassRegistry.isConsistent());

        setDataField(ClassRegistryData.APP_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, appClassRegistry.isReadOnly());
        setDataField(ClassRegistryData.APP_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, appClassRegistry.isConsistent());

        setDataField(ClassRegistryData.DEVICE_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceClassRegistry.isReadOnly());
        setDataField(ClassRegistryData.DEVICE_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, deviceClassRegistry.isConsistent());

        setDataField(ClassRegistryData.GATEWAY_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, gatewayClassRegistry.isReadOnly());
        setDataField(ClassRegistryData.GATEWAY_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, gatewayClassRegistry.isConsistent());
    }

    @Override
    protected void registerRemoteRegistries() {

    }

    /**
     * {@inheritDoc}
     *
     * @param server {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void registerMethods(final RPCServer server) throws CouldNotPerformException {
        super.registerMethods(server);
        server.registerMethods(ClassRegistry.class, this);
    }

    /**
     * Get the internally used agent class registry;
     *
     * @return the internally used agent class registry;
     */
    public ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, ClassRegistryData.Builder> getAgentClassRegistry() {
        return agentClassRegistry;
    }

    /**
     * Get the internally used app class registry;
     *
     * @return the internally used app class registry;
     */
    public ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, ClassRegistryData.Builder> getAppClassRegistry() {
        return appClassRegistry;
    }

    /**
     * Get the internally used device class registry;
     *
     * @return the internally used device class registry;
     */
    public ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, ClassRegistryData.Builder> getDeviceClassRegistry() {
        return deviceClassRegistry;
    }



    /**
     * Get the internally used gateway class registry;
     *
     * @return the internally used gateway class registry;
     */
    public ProtoBufFileSynchronizedRegistry<String, GatewayClass, GatewayClass.Builder, ClassRegistryData.Builder> getGatewayClassRegistry() {
        return gatewayClassRegistry;
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
     */
    @Override
    public DeviceClass getDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRegistry.get(deviceClassId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> registerDeviceClass(DeviceClass deviceClass) {
        return GlobalCachedExecutorService.submit(() -> deviceClassRegistry.register(deviceClass));
    }

    @Override
    public Future<TransactionValue> registerDeviceClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, DeviceClass.class, this::registerDeviceClass);
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
        return deviceClassRegistry.contains(deviceClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceClass(DeviceClass deviceClass) {
        return deviceClassRegistry.contains(deviceClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> updateDeviceClass(DeviceClass deviceClass) {
        return GlobalCachedExecutorService.submit(() -> deviceClassRegistry.update(deviceClass));
    }

    @Override
    public Future<TransactionValue> updateDeviceClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, DeviceClass.class, this::updateDeviceClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> removeDeviceClass(DeviceClass deviceClass) {
        return GlobalCachedExecutorService.submit(() -> deviceClassRegistry.remove(deviceClass));
    }

    @Override
    public Future<TransactionValue> removeDeviceClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, DeviceClass.class, this::removeDeviceClass);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException {
        return deviceClassRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isDeviceClassRegistryReadOnly() {
        return deviceClassRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isDeviceClassRegistryConsistent() {
        return deviceClassRegistry.isConsistent();
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
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.register(agentClass));
    }

    @Override
    public Future<TransactionValue> registerAgentClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, AgentClass.class, this::registerAgentClass);
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
        return agentClassRegistry.contains(agentClass);
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
        return agentClassRegistry.contains(agentClassId);
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
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.update(agentClass));
    }

    @Override
    public Future<TransactionValue> updateAgentClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, AgentClass.class, this::updateAgentClass);
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
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.remove(agentClass));
    }

    @Override
    public Future<TransactionValue> removeAgentClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, AgentClass.class, this::removeAgentClass);
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
        return agentClassRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isAgentClassRegistryReadOnly() {
        return agentClassRegistry.isReadOnly();
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
        return agentClassRegistry.getMessage(agentClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isAgentClassRegistryConsistent() {
        return agentClassRegistry.isConsistent();
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
        return GlobalCachedExecutorService.submit(() -> appClassRegistry.register(appClass));
    }

    @Override
    public Future<TransactionValue> registerAppClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, AppClass.class, this::registerAppClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param appClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException {
        return appClassRegistry.contains(appClass);
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
    public Boolean containsAppClassById(String appClassId) throws CouldNotPerformException {
        return appClassRegistry.contains(appClassId);
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
        return GlobalCachedExecutorService.submit(() -> appClassRegistry.update(appClass));
    }

    @Override
    public Future<TransactionValue> updateAppClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, AppClass.class, this::updateAppClass);
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
        return GlobalCachedExecutorService.submit(() -> appClassRegistry.remove(appClass));
    }

    @Override
    public Future<TransactionValue> removeAppClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, AppClass.class, this::removeAppClass);
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
        return appClassRegistry.getMessage(appClassId);
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
        return appClassRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isAppClassRegistryReadOnly() {
        return appClassRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isAppClassRegistryConsistent() {
        return appClassRegistry.isConsistent();
    }

    // --------------------------------------------- gateway -----------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param gatewayClassId {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public GatewayClass getGatewayClassById(String gatewayClassId) throws CouldNotPerformException {
        return gatewayClassRegistry.get(gatewayClassId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<GatewayClass> registerGatewayClass(GatewayClass gatewayClass) {
        return GlobalCachedExecutorService.submit(() -> gatewayClassRegistry.register(gatewayClass));
    }

    @Override
    public Future<TransactionValue> registerGatewayClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, GatewayClass.class, this::registerGatewayClass);
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
        return gatewayClassRegistry.contains(gatewayClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsGatewayClass(GatewayClass gatewayClass) {
        return gatewayClassRegistry.contains(gatewayClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<GatewayClass> updateGatewayClass(GatewayClass gatewayClass) {
        return GlobalCachedExecutorService.submit(() -> gatewayClassRegistry.update(gatewayClass));
    }

    @Override
    public Future<TransactionValue> updateGatewayClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, GatewayClass.class, this::updateGatewayClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param gatewayClass {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<GatewayClass> removeGatewayClass(GatewayClass gatewayClass) {
        return GlobalCachedExecutorService.submit(() -> gatewayClassRegistry.remove(gatewayClass));
    }

    @Override
    public Future<TransactionValue> removeGatewayClassVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, GatewayClass.class, this::removeGatewayClass);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<GatewayClass> getGatewayClasses() throws CouldNotPerformException {
        return gatewayClassRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isGatewayClassRegistryReadOnly() {
        return gatewayClassRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isGatewayClassRegistryConsistent() {
        return gatewayClassRegistry.isConsistent();
    }
}
