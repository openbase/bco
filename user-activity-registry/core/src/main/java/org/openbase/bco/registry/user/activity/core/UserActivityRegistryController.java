package org.openbase.bco.registry.user.activity.core;

/*
 * #%L
 * BCO Registry User Activity Core
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.lib.generator.UUIDGenerator;
import org.openbase.bco.registry.user.activity.core.consistency.UserActivityConfigClassIdConsistencyHandler;
import org.openbase.bco.registry.user.activity.core.plugin.UserActivityClassCreatorRegistryPlugin;
import org.openbase.bco.registry.user.activity.lib.UserActivityRegistry;
import org.openbase.bco.registry.user.activity.lib.jp.JPUserActivityClassDatabaseDirectory;
import org.openbase.bco.registry.user.activity.lib.jp.JPUserActivityConfigDatabaseDirectory;
import org.openbase.bco.registry.user.activity.lib.jp.JPUserActivityRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.registry.UserActivityRegistryDataType.UserActivityRegistryData;
import rst.domotic.activity.UserActivityClassType.UserActivityClass;
import rst.domotic.activity.UserActivityClassType.UserActivityClass.UserActivityType;
import rst.domotic.activity.UserActivityConfigType.UserActivityConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserActivityRegistryController extends AbstractRegistryController<UserActivityRegistryData, UserActivityRegistryData.Builder> implements UserActivityRegistry, Manageable<ScopeType.Scope> {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserActivityRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserActivityClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserActivityConfig.getDefaultInstance()));
    }
    private final ProtoBufFileSynchronizedRegistry<String, UserActivityClass, UserActivityClass.Builder, UserActivityRegistryData.Builder> userActivityClassRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UserActivityConfig, UserActivityConfig.Builder, UserActivityRegistryData.Builder> userActivityConfigRegistry;
    
    public UserActivityRegistryController() throws InstantiationException, InterruptedException {
        super(JPUserActivityRegistryScope.class, UserActivityRegistryData.newBuilder());
        try {
            this.userActivityClassRegistry = new ProtoBufFileSynchronizedRegistry<>(UserActivityClass.class, getBuilderSetup(), getDataFieldDescriptor(UserActivityRegistryData.USER_ACTIVITY_CLASS_FIELD_NUMBER), new UUIDGenerator<>(), JPService.getProperty(JPUserActivityClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            this.userActivityConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(UserActivityConfig.class, getBuilderSetup(), getDataFieldDescriptor(UserActivityRegistryData.USER_ACTIVITY_CONFIG_FIELD_NUMBER), new UUIDGenerator<>(), JPService.getProperty(JPUserActivityConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
        } catch (JPServiceException | NullPointerException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        registerRegistry(userActivityClassRegistry);
        registerRegistry(userActivityConfigRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        userActivityConfigRegistry.registerConsistencyHandler(new UserActivityConfigClassIdConsistencyHandler(userActivityClassRegistry));
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
        UserActivityClassCreatorRegistryPlugin userActivityClassCreatorRegistryPlugin = new UserActivityClassCreatorRegistryPlugin(userActivityClassRegistry);
        userActivityClassRegistry.registerPlugin(userActivityClassCreatorRegistryPlugin);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        userActivityConfigRegistry.registerDependency(userActivityClassRegistry);
    }
    
    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(UserActivityRegistryData.USER_ACTIVITY_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, userActivityClassRegistry.isConsistent());
        setDataField(UserActivityRegistryData.USER_ACTIVITY_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, userActivityClassRegistry.isReadOnly());
        
        setDataField(UserActivityRegistryData.USER_ACTIVITY_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, userActivityConfigRegistry.isConsistent());
        setDataField(UserActivityRegistryData.USER_ACTIVITY_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userActivityConfigRegistry.isReadOnly());
    }
    
    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);
        RPCHelper.registerInterface(UserActivityRegistry.class, this, server);
    }
    
    @Override
    public Future<UserActivityClass> registerUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> userActivityClassRegistry.register(userActivityClass));
    }
    
    @Override
    public Future<UserActivityClass> updateUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> userActivityClassRegistry.update(userActivityClass));
    }
    
    @Override
    public Future<UserActivityClass> removeUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> userActivityClassRegistry.remove(userActivityClass));
    }
    
    @Override
    public Boolean containsUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException {
        return userActivityClassRegistry.contains(userActivityClass);
    }
    
    @Override
    public Boolean containsUserActivityClassById(String userActivityClassId) throws CouldNotPerformException {
        return userActivityClassRegistry.contains(userActivityClassId);
    }
    
    @Override
    public UserActivityClass getUserActivityClassById(String userActivityClassId) throws CouldNotPerformException {
        return userActivityClassRegistry.getMessage(userActivityClassId);
    }
    
    @Override
    public List<UserActivityClass> getUserActivityClasses() throws CouldNotPerformException {
        return userActivityClassRegistry.getMessages();
    }
    
    @Override
    public Boolean isUserActivityClassRegistryReadOnly() throws CouldNotPerformException {
        return userActivityClassRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isUserActivityClassRegistryConsistent() throws CouldNotPerformException {
        return userActivityClassRegistry.isConsistent();
    }
    
    @Override
    public UserActivityClass getUserActivityClassByType(UserActivityType userActivityType) throws CouldNotPerformException {
        for (UserActivityClass userActivityClass : userActivityClassRegistry.getMessages()) {
            if (userActivityClass.getType() == userActivityType) {
                return userActivityClass;
            }
        }
        throw new NotAvailableException("user activty class " + userActivityType.name());
    }
    
    @Override
    public Future<UserActivityConfig> registerUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> userActivityConfigRegistry.register(userActivityConfig));
    }
    
    @Override
    public Future<UserActivityConfig> updateUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> userActivityConfigRegistry.update(userActivityConfig));
    }
    
    @Override
    public Future<UserActivityConfig> removeUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException {
        return GlobalCachedExecutorService.submit(() -> userActivityConfigRegistry.remove(userActivityConfig));
    }
    
    @Override
    public Boolean containsUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException {
        return userActivityConfigRegistry.contains(userActivityConfig);
    }
    
    @Override
    public Boolean containsUserActivityConfigById(String userActivityConfigId) throws CouldNotPerformException {
        return userActivityConfigRegistry.contains(userActivityConfigId);
    }
    
    @Override
    public UserActivityConfig getUserActivityConfigById(String userActivityConfigId) throws CouldNotPerformException {
        return userActivityConfigRegistry.getMessage(userActivityConfigId);
    }
    
    @Override
    public List<UserActivityConfig> getUserActivityConfigs() throws CouldNotPerformException {
        return userActivityConfigRegistry.getMessages();
    }
    
    @Override
    public Boolean isUserActivityConfigRegistryReadOnly() throws CouldNotPerformException {
        return userActivityConfigRegistry.isReadOnly();
    }
    
    @Override
    public Boolean isUserActivityConfigRegistryConsistent() throws CouldNotPerformException {
        return userActivityConfigRegistry.isConsistent();
    }
    
    @Override
    public List<UserActivityConfig> getUserActivityConfigsByType(UserActivityType userActivityType) throws CouldNotPerformException {
        List<UserActivityConfig> userActivityConfigList = new ArrayList<>();
        
        String userActivityClassId = getUserActivityClassByType(userActivityType).getId();
        for (UserActivityConfig userActivityConfig : userActivityConfigRegistry.getMessages()) {
            if (userActivityConfig.getUserActivityClassId().equals(userActivityClassId)) {
                userActivityConfigList.add(userActivityConfig);
            }
        }
        return userActivityConfigList;
    }
    
}
