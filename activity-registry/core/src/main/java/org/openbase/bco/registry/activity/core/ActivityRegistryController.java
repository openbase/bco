package org.openbase.bco.registry.activity.core;

/*
 * #%L
 * BCO Registry Activity Core
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.registry.activity.core.consistency.ActivityConfigTemplateIdConsistencyHandler;
import org.openbase.bco.registry.activity.lib.ActivityRegistry;
import org.openbase.bco.registry.activity.lib.jp.JPActivityConfigDatabaseDirectory;
import org.openbase.bco.registry.activity.lib.jp.JPActivityRegistryScope;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.lib.com.RegistryVerifiedCommunicationHelper;
import org.openbase.bco.registry.lib.generator.UUIDGenerator;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;
import org.openbase.type.domotic.registry.ActivityRegistryDataType.ActivityRegistryData;
import org.openbase.type.communication.ScopeType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ActivityRegistryController extends AbstractRegistryController<ActivityRegistryData, ActivityRegistryData.Builder> implements ActivityRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TransactionValue.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityConfig.getDefaultInstance()));
    }

    private final ProtoBufFileSynchronizedRegistry<String, ActivityConfig, ActivityConfig.Builder, ActivityRegistryData.Builder> activityConfigRegistry;

    public ActivityRegistryController() throws InstantiationException, InterruptedException {
        super(JPActivityRegistryScope.class, ActivityRegistryData.newBuilder());
        try {
            this.activityConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(ActivityConfig.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(ActivityRegistryData.ACTIVITY_CONFIG_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPActivityConfigDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider);
        } catch (JPServiceException | NullPointerException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerRegistries()  {
        registerRegistry(activityConfigRegistry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerRemoteRegistries()  {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        activityConfigRegistry.registerConsistencyHandler(new ActivityConfigTemplateIdConsistencyHandler());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerPlugins() {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        activityConfigRegistry.registerDependency(CachedTemplateRegistryRemote.getRegistry().getActivityTemplateRemoteRegistry(false));
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException {
        setDataField(ActivityRegistryData.ACTIVITY_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, activityConfigRegistry.isConsistent());
        setDataField(ActivityRegistryData.ACTIVITY_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, activityConfigRegistry.isReadOnly());
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
        RPCHelper.registerInterface(ActivityRegistry.class, this, server);
    }

    /**
     * Get the internally used activity config registry.
     *
     * @return the internally used activity config registry
     */
    public ProtoBufFileSynchronizedRegistry<String, ActivityConfig, ActivityConfig.Builder, ActivityRegistryData.Builder> getActivityConfigRegistry() {
        return activityConfigRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfig {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActivityConfig> registerActivityConfig(ActivityConfig activityConfig)  {
        return GlobalCachedExecutorService.submit(() -> activityConfigRegistry.register(activityConfig));
    }

    @Override
    public Future<TransactionValue> registerActivityConfigVerified(TransactionValue transactionValue)  {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, ActivityConfig.class, this::registerActivityConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfig {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActivityConfig> updateActivityConfig(ActivityConfig activityConfig)  {
        return GlobalCachedExecutorService.submit(() -> activityConfigRegistry.update(activityConfig));
    }

    @Override
    public Future<TransactionValue> updateActivityConfigVerified(TransactionValue transactionValue)  {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, ActivityConfig.class, this::updateActivityConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfig {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActivityConfig> removeActivityConfig(ActivityConfig activityConfig)  {
        return GlobalCachedExecutorService.submit(() -> activityConfigRegistry.remove(activityConfig));
    }

    @Override
    public Future<TransactionValue> removeActivityConfigVerified(TransactionValue transactionValue)  {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, ActivityConfig.class, this::removeActivityConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfig {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsActivityConfig(ActivityConfig activityConfig)  {
        return activityConfigRegistry.contains(activityConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfigId {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsActivityConfigById(String activityConfigId)  {
        return activityConfigRegistry.contains(activityConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ActivityConfig getActivityConfigById(String activityConfigId) throws CouldNotPerformException {
        return activityConfigRegistry.getMessage(activityConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public List<ActivityConfig> getActivityConfigs() throws CouldNotPerformException {
        return activityConfigRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isActivityConfigRegistryReadOnly()  {
        return activityConfigRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isActivityConfigRegistryConsistent()  {
        return activityConfigRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @param activityType {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ActivityConfig> getActivityConfigsByType(final ActivityType activityType) throws CouldNotPerformException {
        final List<ActivityConfig> activityConfigList = new ArrayList<>();

        final String activityTemplateId = CachedTemplateRegistryRemote.getRegistry().getActivityTemplateByType(activityType).getId();
        for (final ActivityConfig activityConfig : activityConfigRegistry.getMessages()) {
            if (activityConfig.getActivityTemplateId().equals(activityTemplateId)) {
                activityConfigList.add(activityConfig);
            }
        }
        return activityConfigList;
    }
}
