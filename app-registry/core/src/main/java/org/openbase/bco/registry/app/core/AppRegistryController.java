package org.openbase.bco.registry.app.core;

/*
 * #%L
 * REM AppRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.concurrent.Future;
import org.openbase.bco.registry.app.core.consistency.LabelConsistencyHandler;
import org.openbase.bco.registry.app.core.consistency.ScopeConsistencyHandler;
import org.openbase.bco.registry.app.lib.AppRegistry;
import org.openbase.bco.registry.app.lib.generator.AppConfigIdGenerator;
import org.openbase.bco.registry.app.lib.jp.JPAppConfigDatabaseDirectory;
import org.openbase.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.user.core.dbconvert.DummyConverter;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryDataType.AppRegistryData;
import rst.rsb.ScopeType;
import rst.spatial.LocationRegistryDataType.LocationRegistryData;

/**
 *
 * @author mpohling
 */
public class AppRegistryController extends RSBCommunicationService<AppRegistryData, AppRegistryData.Builder> implements AppRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AppConfig, AppConfig.Builder, AppRegistryData.Builder> appConfigRegistry;

    private final LocationRegistryRemote locationRegistryRemote;
    private Observer<LocationRegistryData> locationRegistryUpdateObserver;

    public AppRegistryController() throws InstantiationException, InterruptedException {
        super(AppRegistryData.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            appConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(AppConfig.class, getBuilderSetup(), getDataFieldDescriptor(AppRegistryData.APP_CONFIG_FIELD_NUMBER), new AppConfigIdGenerator(), JPService.getProperty(JPAppConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            appConfigRegistry.activateVersionControl(DummyConverter.class.getPackage());

            locationRegistryUpdateObserver = new Observer<LocationRegistryData>() {

                @Override
                public void update(Observable<LocationRegistryData> source, LocationRegistryData data) throws Exception {
                    appConfigRegistry.checkConsistency();
                }
            };

            locationRegistryRemote = new LocationRegistryRemote();

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
            locationRegistryRemote.waitForData();
            locationRegistryRemote.addDataObserver(locationRegistryUpdateObserver);
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
        locationRegistryRemote.removeDataObserver(locationRegistryUpdateObserver);
        locationRegistryRemote.deactivate();
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
        locationRegistryRemote.shutdown();
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException, InterruptedException {
        // sync read only flags
        setDataField(AppRegistryData.APP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, appConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(AppRegistry.class, this, server);
    }

    @Override
    public Future<AppConfig> registerAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> appConfigRegistry.register(appConfig));
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
        return GlobalExecutionService.submit(() -> appConfigRegistry.update(appConfig));
    }

    @Override
    public Future<AppConfig> removeAppConfig(AppConfig appConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> appConfigRegistry.remove(appConfig));
    }

    @Override
    public List<AppConfig> getAppConfigs() throws CouldNotPerformException {
        return appConfigRegistry.getMessages();
    }

    @Override
    public Boolean isAppConfigRegistryReadOnly() throws CouldNotPerformException {
        return appConfigRegistry.isReadOnly();
    }

    public ProtoBufFileSynchronizedRegistry<String, AppConfig, AppConfig.Builder, AppRegistryData.Builder> getAppConfigRegistry() {
        return appConfigRegistry;
    }
}
