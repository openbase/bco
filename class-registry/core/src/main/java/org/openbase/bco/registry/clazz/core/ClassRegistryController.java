package org.openbase.bco.registry.clazz.core;

import org.openbase.bco.registry.clazz.core.consistency.DeviceClassRequiredFieldConsistencyHandler;
import org.openbase.bco.registry.clazz.core.consistency.UnitTemplateConfigIdConsistencyHandler;
import org.openbase.bco.registry.clazz.core.consistency.UnitTemplateConfigLabelConsistencyHandler;
import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.clazz.lib.jp.JPAgentClassDatabaseDirectory;
import org.openbase.bco.registry.clazz.lib.jp.JPAppClassDatabaseDirectory;
import org.openbase.bco.registry.clazz.lib.jp.JPClassRegistryScope;
import org.openbase.bco.registry.clazz.lib.jp.JPDeviceClassDatabaseDirectory;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.lib.generator.UUIDGenerator;
import org.openbase.bco.registry.lib.provider.DeviceClassCollectionProvider;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.registry.ClassRegistryDataType.ClassRegistryData;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.List;
import java.util.concurrent.Future;

/*
 * #%L
 * BCO Registry Class Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
public class ClassRegistryController extends AbstractRegistryController<ClassRegistryData, ClassRegistryData.Builder> implements ClassRegistry, DataProvider<ClassRegistryData>, DeviceClassCollectionProvider, Shutdownable {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ClassRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClass.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, ClassRegistryData.Builder> agentClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, ClassRegistryData.Builder> appClassRegistry;
    private ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, ClassRegistryData.Builder> deviceClassRegistry;

    public ClassRegistryController() throws InstantiationException, InterruptedException {
        super(JPClassRegistryScope.class, ClassRegistryData.newBuilder(), SPARSELY_REGISTRY_DATA_NOTIFIED);
        try {
            agentClassRegistry = new ProtoBufFileSynchronizedRegistry<>(AgentClass.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(ClassRegistryData.AGENT_CLASS_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPAgentClassDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider);

            appClassRegistry = new ProtoBufFileSynchronizedRegistry<>(AppClass.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(ClassRegistryData.APP_CLASS_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPAppClassDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider);

            deviceClassRegistry = new ProtoBufFileSynchronizedRegistry<>(DeviceClass.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(ClassRegistryData.DEVICE_CLASS_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPDeviceClassDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider);
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        deviceClassRegistry.registerConsistencyHandler(new UnitTemplateConfigIdConsistencyHandler());
        deviceClassRegistry.registerConsistencyHandler(new UnitTemplateConfigLabelConsistencyHandler());
        deviceClassRegistry.registerConsistencyHandler(new DeviceClassRequiredFieldConsistencyHandler());
    }

    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        agentClassRegistry.registerDependency(CachedTemplateRegistryRemote.getRegistry().getUnitTemplateRemoteRegistry());
        appClassRegistry.registerDependency(CachedTemplateRegistryRemote.getRegistry().getUnitTemplateRemoteRegistry());
        deviceClassRegistry.registerDependency(CachedTemplateRegistryRemote.getRegistry().getUnitTemplateRemoteRegistry());
    }

    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
    }

    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        registerRegistry(agentClassRegistry);
        registerRegistry(appClassRegistry);
        registerRegistry(deviceClassRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException       {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(ClassRegistryData.AGENT_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, agentClassRegistry.isReadOnly());
        setDataField(ClassRegistryData.AGENT_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, agentClassRegistry.isConsistent());

        setDataField(ClassRegistryData.APP_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, appClassRegistry.isReadOnly());
        setDataField(ClassRegistryData.APP_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, appClassRegistry.isConsistent());

        setDataField(ClassRegistryData.DEVICE_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, deviceClassRegistry.isReadOnly());
        setDataField(ClassRegistryData.DEVICE_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, deviceClassRegistry.isConsistent());
    }

    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {

    }

    /**
     * {@inheritDoc}
     *
     * @param server {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);
        RPCHelper.registerInterface(ClassRegistry.class, this, server);
    }

    public ProtoBufFileSynchronizedRegistry<String, AgentClass, AgentClass.Builder, ClassRegistryData.Builder> getAgentClassRegistry() {
        return agentClassRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, ClassRegistryData.Builder> getAppClassRegistry() {
        return appClassRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, ClassRegistryData.Builder> getDeviceClassRegistry() {
        return deviceClassRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId {@inheritDoc}
     * @return {@inheritDoc}
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
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> registerDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> deviceClassRegistry.register(deviceClass));
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        return deviceClassRegistry.contains(deviceClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean containsDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return deviceClassRegistry.contains(deviceClass);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<DeviceClass> updateDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> deviceClassRegistry.update(deviceClass));
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Future<DeviceClass> removeDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> deviceClassRegistry.remove(deviceClass));
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException {
        return deviceClassRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException {
        return deviceClassRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException {
        return deviceClassRegistry.isConsistent();
    }

    @Override
    public Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.register(agentClass));
    }

    @Override
    public Boolean containsAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return agentClassRegistry.contains(agentClass);
    }

    @Override
    public Boolean containsAgentClassById(String agentClassId) throws CouldNotPerformException {
        return agentClassRegistry.contains(agentClassId);
    }

    @Override
    public Future<AgentClass> updateAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.update(agentClass));
    }

    @Override
    public Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> agentClassRegistry.remove(agentClass));
    }

    @Override
    public List<AgentClass> getAgentClasses() throws CouldNotPerformException {
        return agentClassRegistry.getMessages();
    }

    @Override
    public Boolean isAgentClassRegistryReadOnly() throws CouldNotPerformException {
        return agentClassRegistry.isReadOnly();
    }

    @Override
    public AgentClass getAgentClassById(String agentClassId) throws CouldNotPerformException {
        return agentClassRegistry.getMessage(agentClassId);
    }

    @Override
    public Boolean isAgentClassRegistryConsistent() throws CouldNotPerformException {
        return agentClassRegistry.isConsistent();
    }

    @Override
    public Future<AppClass> registerAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> appClassRegistry.register(appClass));
    }

    @Override
    public Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.contains(appClass);
    }

    @Override
    public Boolean containsAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.contains(appClassId);
    }

    @Override
    public Future<AppClass> updateAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> appClassRegistry.update(appClass));
    }

    @Override
    public Future<AppClass> removeAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> appClassRegistry.remove(appClass));
    }

    @Override
    public AppClass getAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.getMessage(appClassId);
    }

    @Override
    public List<AppClass> getAppClasses() throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.getMessages();
    }

    @Override
    public Boolean isAppClassRegistryReadOnly() throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.isReadOnly();
    }

    @Override
    public Boolean isAppClassRegistryConsistent() throws CouldNotPerformException {
        return appClassRegistry.isConsistent();
    }
}
