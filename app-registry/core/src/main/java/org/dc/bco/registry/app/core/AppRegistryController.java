package org.dc.bco.registry.app.core;

/*
 * #%L
 * REM AppRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import org.dc.bco.registry.app.core.consistency.LabelConsistencyHandler;
import org.dc.bco.registry.app.core.consistency.ScopeConsistencyHandler;
import org.dc.bco.registry.app.lib.generator.AppConfigIdGenerator;
import org.dc.bco.registry.app.lib.jp.JPAppConfigDatabaseDirectory;
import org.dc.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
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
import org.dc.jul.iface.Manageable;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.storage.file.ProtoBufJSonFileProvider;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.app.AppConfigType;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryType.AppRegistry;
import rst.rsb.ScopeType;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class AppRegistryController extends RSBCommunicationService<AppRegistry, AppRegistry.Builder> implements org.dc.bco.registry.app.lib.AppRegistry, Manageable<ScopeType.Scope> {

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
            appConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(AppConfig.class, getBuilderSetup(), getDataFieldDescriptor(AppRegistry.APP_CONFIG_FIELD_NUMBER), new AppConfigIdGenerator(), JPService.getProperty(JPAppConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            locationRegistryUpdateObserver = new Observer<LocationRegistry>() {

                @Override
                public void update(Observable<LocationRegistry> source, LocationRegistry data) throws Exception {
                    appConfigRegistry.checkConsistency();
                }
            };

            locationRegistryRemote = new LocationRegistryRemote();

            appConfigRegistry.setName("AppConfigRegistry");
            
            appConfigRegistry.loadRegistry();

            appConfigRegistry.registerConsistencyHandler(new ScopeConsistencyHandler(locationRegistryRemote));
            appConfigRegistry.registerConsistencyHandler(new LabelConsistencyHandler());
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

    public void init() throws InitializationException, InterruptedException {
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
        setDataField(AppRegistry.APP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, appConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(org.dc.bco.registry.app.lib.AppRegistry.class, this, server);
    }

    @Override
    public Future<AppConfig> registerAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return ForkJoinPool.commonPool().submit(() -> appConfigRegistry.register(appConfig));
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
    public Future<AppConfig> updateAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return ForkJoinPool.commonPool().submit(() -> appConfigRegistry.update(appConfig));
    }

    @Override
    public Future<AppConfig> removeAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return ForkJoinPool.commonPool().submit(() -> appConfigRegistry.remove(appConfig));
    }

    @Override
    public List<AppConfig> getAppConfigs() throws CouldNotPerformException {
        return appConfigRegistry.getMessages();
    }

    @Override
    public Boolean isAppConfigRegistryReadOnly() throws CouldNotPerformException {
        return appConfigRegistry.isReadOnly();
    }

    public ProtoBufFileSynchronizedRegistry<String, AppConfig, AppConfig.Builder, AppRegistry.Builder> getAppConfigRegistry() {
        return appConfigRegistry;
    }
}
