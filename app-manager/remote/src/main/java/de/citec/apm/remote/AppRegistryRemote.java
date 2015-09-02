/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.apm.remote;

import de.citec.apm.lib.generator.AppConfigIdGenerator;
import de.citec.apm.lib.registry.AppRegistryInterface;
import de.citec.jp.JPAppRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import de.citec.jul.storage.registry.RemoteRegistry;
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
public class AppRegistryRemote extends RSBRemoteService<AppRegistry> implements AppRegistryInterface {

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
        super.init(JPService.getProperty(JPAppRegistryScope.class).getValue());
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch(CouldNotPerformException ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Initial registry sync failed!", ex));
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
        if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
            return CompletableFuture.completedFuture(true);
        }
        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the app config registry!!", ex);
        }
    }
}
