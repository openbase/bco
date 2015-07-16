/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.apm.core.registry;

import de.citec.apm.lib.generator.AppConfigIdGenerator;
import de.citec.apm.lib.registry.AppRegistryInterface;
import de.citec.jp.JPAppConfigDatabaseDirectory;
import de.citec.jp.JPAppRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import java.util.List;
import java.util.Map;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.util.RPCHelper;
import de.citec.lm.remote.LocationRegistryRemote;
import rst.homeautomation.control.app.AppConfigType;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryType.AppRegistry;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class AppRegistryService extends RSBCommunicationService<AppRegistry, AppRegistry.Builder> implements AppRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfigType.AppConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AppConfig, AppConfig.Builder, AppRegistry.Builder> appConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private Observer<LocationRegistry> locationRegistryUpdateObserver;

    public AppRegistryService() throws InstantiationException, InterruptedException {
        super(JPService.getProperty(JPAppRegistryScope.class).getValue(), AppRegistry.newBuilder());
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

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException {
        super.init();
        locationRegistryRemote.init();
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
            ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
        }
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(AppRegistryInterface.class, this, server);
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
}
