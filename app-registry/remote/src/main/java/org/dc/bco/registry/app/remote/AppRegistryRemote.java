/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.remote;

import org.dc.bco.registry.app.lib.generator.AppConfigIdGenerator;
import org.dc.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.storage.registry.RemoteRegistry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.app.AppConfigType;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryType.AppRegistry;

/**
 *
 * @author mpohling
 */
public class AppRegistryRemote extends RSBRemoteService<AppRegistry> implements org.dc.bco.registry.app.lib.AppRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfigType.AppConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, AppConfig, AppConfig.Builder, AppRegistry.Builder> appConfigRemoteRegistry;

    public AppRegistryRemote() throws InstantiationException {
        try {
            appConfigRemoteRegistry = new RemoteRegistry<>(new AppConfigIdGenerator());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            super.init(JPService.getProperty(JPAppRegistryScope.class).getValue());
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
