package org.openbase.bco.registry.activity.remote;

/*
 * #%L
 * BCO Registry Activity Remote
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

import org.openbase.bco.registry.activity.lib.ActivityRegistry;
import org.openbase.bco.registry.activity.lib.jp.JPActivityRegistryScope;
import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.RegistryVerifiedCommunicationHelper;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.pattern.Remote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.activity.ActivityConfigType.ActivityConfig;
import rst.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;
import rst.domotic.communication.TransactionValueType.TransactionValue;
import rst.domotic.registry.ActivityRegistryDataType.ActivityRegistryData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ActivityRegistryRemote extends AbstractRegistryRemote<ActivityRegistryData> implements ActivityRegistry, Remote<ActivityRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityConfig.getDefaultInstance()));
    }

    private final SynchronizedRemoteRegistry<String, ActivityConfig, ActivityConfig.Builder> activityConfigRemoteRegistry;

    public ActivityRegistryRemote() throws InstantiationException, InterruptedException {
        super(JPActivityRegistryScope.class, ActivityRegistryData.class);
        try {
            this.activityConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, ActivityRegistryData.ACTIVITY_CONFIG_FIELD_NUMBER);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.InitializationException {@inheritDoc}
     */
    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        /* ATTENTION: the order here is important, if somebody registers an observer
         * on one of these remote registries and tries to get values from other remote registries
         * which are registered later than these are not synced yet
         */
        registerRemoteRegistry(activityConfigRemoteRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<ActivityConfig> registerActivityConfig(ActivityConfig activityConfig) throws CouldNotPerformException {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(activityConfig, this, this::registerActivityConfigVerified);
    }

    @Override
    public Future<TransactionValue> registerActivityConfigVerified(TransactionValue transactionValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(transactionValue, this, TransactionValue.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<ActivityConfig> updateActivityConfig(ActivityConfig activityConfig) throws CouldNotPerformException {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(activityConfig, this, this::updateActivityConfigVerified);
    }

    @Override
    public Future<TransactionValue> updateActivityConfigVerified(TransactionValue transactionValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(transactionValue, this, TransactionValue.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<ActivityConfig> removeActivityConfig(ActivityConfig activityConfig) throws CouldNotPerformException {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(activityConfig, this, this::removeActivityConfigVerified);
    }

    @Override
    public Future<TransactionValue> removeActivityConfigVerified(TransactionValue transactionValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(transactionValue, this, TransactionValue.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfig {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsActivityConfig(ActivityConfig activityConfig) throws CouldNotPerformException {
        validateData();
        return activityConfigRemoteRegistry.contains(activityConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsActivityConfigById(String activityConfigId) throws CouldNotPerformException {
        validateData();
        return activityConfigRemoteRegistry.contains(activityConfigId);
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
        validateData();
        return activityConfigRemoteRegistry.getMessage(activityConfigId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ActivityConfig> getActivityConfigs() throws CouldNotPerformException {
        validateData();
        return activityConfigRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isActivityConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getActivityConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isActivityConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getActivityConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param activityType{@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ActivityConfig> getActivityConfigsByType(ActivityType activityType) throws CouldNotPerformException {
        List<ActivityConfig> activityConfigList = new ArrayList<>();

        String activityTemplateId = CachedTemplateRegistryRemote.getRegistry().getActivityTemplateByType(activityType).getId();
        for (ActivityConfig activityConfig : getActivityConfigs()) {
            if (activityConfig.getActivityTemplateId().equals(activityTemplateId)) {
                activityConfigList.add(activityConfig);
            }
        }
        return activityConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isConsistent() throws CouldNotPerformException {
        return isActivityConfigRegistryConsistent();
    }
}
