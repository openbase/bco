/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.remote;

/*
 * #%L
 * REM AppRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.dc.jul.pattern.Remote;
import org.dc.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.app.AppConfigType;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryType.AppRegistry;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class AppRegistryRemote extends RSBRemoteService<AppRegistry> implements org.dc.bco.registry.app.lib.AppRegistry, Remote<AppRegistry> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfigType.AppConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, AppConfig, AppConfig.Builder, AppRegistry.Builder> appConfigRemoteRegistry;

    public AppRegistryRemote() throws InstantiationException, InterruptedException {
        try {
            appConfigRemoteRegistry = new RemoteRegistry<>();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public  void init(final Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotTransformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public synchronized void init(final ScopeType.Scope scope) throws InitializationException, InterruptedException {
        super.init(scope);
    }


    /**
     * Method initializes the remote with the default registry connection scope.
     *
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    public void init() throws InitializationException, InterruptedException {
        try {
            this.init(JPService.getProperty(JPAppRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void notifyUpdated(final AppRegistry data) throws CouldNotPerformException {
        appConfigRemoteRegistry.notifyRegistryUpdated(data.getAppConfigList());
    }

    public RemoteRegistry<String, AppConfig, AppConfig.Builder, AppRegistry.Builder> getAppConfigRemoteRegistry() {
        return appConfigRemoteRegistry;
    }

    @Override
    public AppConfigType.AppConfig registerAppConfig(final AppConfigType.AppConfig appConfig) throws CouldNotPerformException {
        try {
            return (AppConfigType.AppConfig) callMethod("registerAppConfig", appConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register app config!", ex);
        }
    }

    @Override
    public AppConfig getAppConfigById(String appConfigId) throws CouldNotPerformException, NotAvailableException {
        getData();
        return appConfigRemoteRegistry.getMessage(appConfigId);
    }

    @Override
    public Boolean containsAppConfig(final AppConfigType.AppConfig appConfig) throws CouldNotPerformException {
        getData();
        return appConfigRemoteRegistry.contains(appConfig);
    }

    @Override
    public Boolean containsAppConfigById(final String appConfigId) throws CouldNotPerformException {
        getData();
        return appConfigRemoteRegistry.contains(appConfigId);
    }

    @Override
    public AppConfigType.AppConfig updateAppConfig(final AppConfigType.AppConfig appConfig) throws CouldNotPerformException {
        try {
            return (AppConfigType.AppConfig) callMethod("updateAppConfig", appConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update app config!", ex);
        }
    }

    @Override
    public AppConfigType.AppConfig removeAppConfig(final AppConfigType.AppConfig appConfig) throws CouldNotPerformException {
        try {
            return (AppConfigType.AppConfig) callMethod("removeAppConfig", appConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove app config!", ex);
        }
    }

    @Override
    public List<AppConfig> getAppConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<AppConfig> messages = appConfigRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public Future<Boolean> isAppConfigRegistryReadOnly() throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the app config registry!!", ex);
        }
    }
}
