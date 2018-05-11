package org.openbase.bco.registry.clazz.remote;

/*
 * #%L
 * BCO Registry Class Remote
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

import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.clazz.lib.jp.JPClassRegistryScope;
import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.lib.provider.DeviceClassCollectionProvider;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.RegistryRemote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.registry.ClassRegistryDataType.ClassRegistryData;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ClassRegistryRemote extends AbstractRegistryRemote<ClassRegistryData> implements ClassRegistry, RegistryRemote<ClassRegistryData>, DataProvider<ClassRegistryData>, DeviceClassCollectionProvider {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ClassRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClass.getDefaultInstance()));
    }

    private final SynchronizedRemoteRegistry<String, AgentClass, AgentClass.Builder> agentClassRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, AppClass, AppClass.Builder> appClassRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, DeviceClass, DeviceClass.Builder> deviceClassRemoteRegistry;

    public ClassRegistryRemote() throws InstantiationException {
        super(JPClassRegistryScope.class, ClassRegistryData.class);
        try {
            agentClassRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, ClassRegistryData.AGENT_CLASS_FIELD_NUMBER);
            appClassRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, ClassRegistryData.APP_CLASS_FIELD_NUMBER);
            deviceClassRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, ClassRegistryData.DEVICE_CLASS_FIELD_NUMBER);
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
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(agentClassRemoteRegistry);
        registerRemoteRegistry(appClassRemoteRegistry);
        registerRemoteRegistry(deviceClassRemoteRegistry);
    }

    public SynchronizedRemoteRegistry<String, AgentClass, AgentClass.Builder> getAgentClassRemoteRegistry() {
        return agentClassRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, AppClass, AppClass.Builder> getAppClassRemoteRegistry() {
        return appClassRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, DeviceClass, DeviceClass.Builder> getDeviceClassRemoteRegistry() {
        return deviceClassRemoteRegistry;
    }

    @Override
    public Boolean isConsistent() throws CouldNotPerformException {
        return isDeviceClassRegistryConsistent()
                && isAgentClassRegistryConsistent()
                && isAppClassRegistryConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClassId {@inheritDoc}
     * @return {@inheritDoc}
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
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(deviceClass, this, DeviceClass.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        validateData();
        return deviceClassRemoteRegistry.contains(deviceClass);
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
        validateData();
        return deviceClassRemoteRegistry.contains(deviceClassId);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(deviceClass, this, DeviceClass.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param deviceClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(deviceClass, this, DeviceClass.class);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
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
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        validateData();
        return getData().getDeviceClassRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getDeviceClassRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(agentClass, this, AgentClass.class);
    }

    @Override
    public Boolean containsAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.contains(agentClass);
    }

    @Override
    public Boolean containsAgentClassById(String agentClassId) throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.contains(agentClassId);
    }

    @Override
    public Future<AgentClass> updateAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(agentClass, this, AgentClass.class);
    }

    @Override
    public Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(agentClass, this, AgentClass.class);
    }

    @Override
    public List<AgentClass> getAgentClasses() throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isAgentClassRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        return getData().getAgentClassRegistryReadOnly();
    }

    @Override
    public AgentClass getAgentClassById(String agentClassId) throws CouldNotPerformException {
        validateData();
        return agentClassRemoteRegistry.getMessage(agentClassId);
    }

    @Override
    public Boolean isAgentClassRegistryConsistent() throws CouldNotPerformException {
        validateData();
        return getData().getAgentClassRegistryConsistent();
    }

    @Override
    public Future<AppClass> registerAppClass(AppClass appClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(appClass, this, AppClass.class);
    }

    @Override
    public Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException {
        validateData();
        return appClassRemoteRegistry.contains(appClass);
    }

    @Override
    public Boolean containsAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException {
        validateData();
        return appClassRemoteRegistry.contains(appClassId);
    }

    @Override
    public Future<AppClass> updateAppClass(AppClass appClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(appClass, this, AppClass.class);
    }

    @Override
    public Future<AppClass> removeAppClass(AppClass appClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(appClass, this, AppClass.class);
    }

    @Override
    public AppClass getAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException {
        validateData();
        return appClassRemoteRegistry.getMessage(appClassId);
    }

    @Override
    public List<AppClass> getAppClasses() throws CouldNotPerformException, InterruptedException {
        validateData();
        return appClassRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isAppClassRegistryReadOnly() throws CouldNotPerformException, InterruptedException {
        validateData();
        return getData().getAppClassRegistryReadOnly();
    }

    @Override
    public Boolean isAppClassRegistryConsistent() throws CouldNotPerformException {
        validateData();
        return getData().getAppClassRegistryConsistent();
    }
}
