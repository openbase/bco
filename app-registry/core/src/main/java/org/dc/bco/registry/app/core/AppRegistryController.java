/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.core;

import org.dc.bco.registry.app.lib.generator.AppConfigIdGenerator;
import org.dc.bco.registry.app.lib.jp.JPAppConfigDatabaseDirectory;
import org.dc.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.storage.file.ProtoBufJSonFileProvider;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.app.AppConfigType;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryType.AppRegistry;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class AppRegistryController extends RSBCommunicationService<AppRegistry, AppRegistry.Builder> implements org.dc.bco.registry.app.lib.AppRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfigType.AppConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AppConfig, AppConfig.Builder, AppRegistry.Builder> appConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private Observer<LocationRegistry> locationRegistryUpdateObserver;

    public AppRegistryController() throws InstantiationException, InterruptedException {
        super(AppRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            appConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(AppConfig.class, getBuilderSetup(), getFieldDescriptor(AppRegistry.APP_CONFIG_FIELD_NUMBER), new AppConfigIdGenerator(), JPService.getProperty(JPAppConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            locationRegistryUpdateObserver = new Observer<LocationRegistry>() {

                @Override
                public void update(Observable<LocationRegistry> source, LocationRegistry data) throws Exception {
                    appConfigRegistry.checkConsistency();
                }
            };

            locationRegistryRemote = new LocationRegistryRemote();

            appConfigRegistry.loadRegistry();

            appConfigRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, AppConfig, AppConfig.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, AppConfig, AppConfig.Builder>>> source, Map<String, IdentifiableMessage<String, AppConfig, AppConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            super.init(JPService.getProperty(JPAppRegistryScope.class).getValue());
            locationRegistryRemote.init();
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            locationRegistryRemote.activate();
            locationRegistryRemote.addObserver(locationRegistryUpdateObserver);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            appConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            logger.warn("Initial consistency check failed!");
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        locationRegistryRemote.removeObserver(locationRegistryUpdateObserver);
        super.deactivate();
    }

    @Override
    public void shutdown() {
        if (appConfigRegistry != null) {
            appConfigRegistry.shutdown();
        }

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setField(AppRegistry.APP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, appConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(org.dc.bco.registry.app.lib.AppRegistry.class, this, server);
    }

    @Override
    public AppConfig registerAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return appConfigRegistry.register(appConfig);
    }

    @Override
    public AppConfig getAppConfigById(String appConfigId) throws CouldNotPerformException {
        return appConfigRegistry.get(appConfigId).getMessage();
    }

    @Override
    public Boolean containsAppConfigById(String appConfigId) throws CouldNotPerformException {
        return appConfigRegistry.contains(appConfigId);
    }

    @Override
    public Boolean containsAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return appConfigRegistry.contains(appConfig);
    }

    @Override
    public AppConfig updateAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return appConfigRegistry.update(appConfig);
    }

    @Override
    public AppConfig removeAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return appConfigRegistry.remove(appConfig);
    }

    @Override
    public List<AppConfig> getAppConfigs() throws CouldNotPerformException {
        return appConfigRegistry.getMessages();
    }

    @Override
    public Future<Boolean> isAppConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(appConfigRegistry.isReadOnly());
    }

    public ProtoBufFileSynchronizedRegistry<String, AppConfig, AppConfig.Builder, AppRegistry.Builder> getAppConfigRegistry() {
        return appConfigRegistry;
    }
}
