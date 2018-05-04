package org.openbase.bco.registry.user.activity.remote;

/*
 * #%L
 * BCO Registry User Activity Remote
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

import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.user.activity.lib.UserActivityRegistry;
import org.openbase.bco.registry.user.activity.lib.jp.JPUserActivityRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.pattern.Remote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.activity.UserActivityClassType.UserActivityClass;
import rst.domotic.activity.UserActivityClassType.UserActivityClass.UserActivityType;
import rst.domotic.activity.UserActivityConfigType.UserActivityConfig;
import rst.domotic.registry.UserActivityRegistryDataType.UserActivityRegistryData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserActivityRegistryRemote extends AbstractRegistryRemote<UserActivityRegistryData> implements UserActivityRegistry, Remote<UserActivityRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserActivityRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserActivityClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserActivityConfig.getDefaultInstance()));
    }

    private final SynchronizedRemoteRegistry<String, UserActivityClass, UserActivityClass.Builder> userActivityClassRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UserActivityConfig, UserActivityConfig.Builder> userActivityConfigRemoteRegistry;

    public UserActivityRegistryRemote() throws InstantiationException, InterruptedException {
        super(JPUserActivityRegistryScope.class, UserActivityRegistryData.class);
        try {
            this.userActivityClassRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UserActivityRegistryData.USER_ACTIVITY_CLASS_FIELD_NUMBER);
            this.userActivityConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, UserActivityRegistryData.USER_ACTIVITY_CONFIG_FIELD_NUMBER);
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
        registerRemoteRegistry(userActivityClassRemoteRegistry);
        registerRemoteRegistry(userActivityConfigRemoteRegistry);
    }

    @Override
    public Future<UserActivityClass> registerUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userActivityClass, this, UserActivityClass.class);
    }

    @Override
    public Future<UserActivityClass> updateUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userActivityClass, this, UserActivityClass.class);
    }

    @Override
    public Future<UserActivityClass> removeUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userActivityClass, this, UserActivityClass.class);
    }

    @Override
    public Boolean containsUserActivityClass(UserActivityClass userActivityClass) throws CouldNotPerformException {
        validateData();
        return userActivityClassRemoteRegistry.contains(userActivityClass);
    }

    @Override
    public Boolean containsUserActivityClassById(String userActivityClassId) throws CouldNotPerformException {
        validateData();
        return userActivityClassRemoteRegistry.contains(userActivityClassId);
    }

    @Override
    public UserActivityClass getUserActivityClassById(String userActivityClassId) throws CouldNotPerformException {
        validateData();
        return userActivityClassRemoteRegistry.getMessage(userActivityClassId);
    }

    @Override
    public List<UserActivityClass> getUserActivityClasses() throws CouldNotPerformException {
        validateData();
        return userActivityClassRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isUserActivityClassRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUserActivityClassRegistryReadOnly();
    }

    @Override
    public Boolean isUserActivityClassRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUserActivityClassRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public UserActivityClass getUserActivityClassByType(UserActivityType userActivityType) throws CouldNotPerformException {
        validateData();
        for (UserActivityClass userActivityClass : userActivityClassRemoteRegistry.getMessages()) {
            if (userActivityClass.getType() == userActivityType) {
                return userActivityClass;
            }
        }
        throw new NotAvailableException("user activty class " + userActivityType.name());
    }

    @Override
    public Future<UserActivityConfig> registerUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userActivityConfig, this, UserActivityConfig.class);
    }

    @Override
    public Future<UserActivityConfig> updateUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userActivityConfig, this, UserActivityConfig.class);
    }

    @Override
    public Future<UserActivityConfig> removeUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userActivityConfig, this, UserActivityConfig.class);
    }

    @Override
    public Boolean containsUserActivityConfig(UserActivityConfig userActivityConfig) throws CouldNotPerformException {
        validateData();
        return userActivityConfigRemoteRegistry.contains(userActivityConfig);
    }

    @Override
    public Boolean containsUserActivityConfigById(String userActivityConfigId) throws CouldNotPerformException {
        validateData();
        return userActivityConfigRemoteRegistry.contains(userActivityConfigId);
    }

    @Override
    public UserActivityConfig getUserActivityConfigById(String userActivityConfigId) throws CouldNotPerformException {
        validateData();
        return userActivityConfigRemoteRegistry.getMessage(userActivityConfigId);
    }

    @Override
    public List<UserActivityConfig> getUserActivityConfigs() throws CouldNotPerformException {
        validateData();
        return userActivityConfigRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isUserActivityConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUserActivityConfigRegistryReadOnly();
    }

    @Override
    public Boolean isUserActivityConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUserActivityConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public List<UserActivityConfig> getUserActivityConfigsByType(UserActivityType userActivityType) throws CouldNotPerformException {
        validateData();
        List<UserActivityConfig> userActivityConfigList = new ArrayList<>();

        String userActivityClassId = getUserActivityClassByType(userActivityType).getId();
        for (UserActivityConfig userActivityConfig : userActivityConfigRemoteRegistry.getMessages()) {
            if (userActivityConfig.getUserActivityClassId().equals(userActivityClassId)) {
                userActivityConfigList.add(userActivityConfig);
            }
        }
        return userActivityConfigList;
    }

    @Override
    public Boolean isConsistent() throws CouldNotPerformException {
        return isUserActivityClassRegistryConsistent() && isUserActivityConfigRegistryConsistent();
    }
}
