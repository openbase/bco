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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.registry.app.core.dbconvert.DummyConverter;
import org.openbase.bco.registry.app.lib.AppRegistry;
import org.openbase.bco.registry.app.lib.generator.AppClassIdGenerator;
import org.openbase.bco.registry.app.lib.jp.JPAppClassDatabaseDirectory;
import org.openbase.bco.registry.app.lib.jp.JPAppRegistryScope;
import org.openbase.bco.registry.lib.controller.AbstractRegistryController;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.app.AppClassType.AppClass;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryDataType.AppRegistryData;
import rst.rsb.ScopeType;

/**
 *
 * @author mpohling
 */
public class AppRegistryController extends AbstractRegistryController<AppRegistryData, AppRegistryData.Builder> implements AppRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppClass.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, AppRegistryData.Builder> appClassRegistry;

    private final LocationRegistryRemote locationRegistryRemote;

    public AppRegistryController() throws InstantiationException, InterruptedException {
        super(JPAppRegistryScope.class, AppRegistryData.newBuilder());
        try {
            appClassRegistry = new ProtoBufFileSynchronizedRegistry<>(AppClass.class, getBuilderSetup(), getDataFieldDescriptor(AppRegistryData.APP_CLASS_FIELD_NUMBER), new AppClassIdGenerator(), JPService.getProperty(JPAppClassDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            locationRegistryRemote = new LocationRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void activateVersionControl() throws CouldNotPerformException {
        appClassRegistry.activateVersionControl(DummyConverter.class.getPackage());
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void loadRegistries() throws CouldNotPerformException {
        appClassRegistry.loadRegistry();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistryRemotes() throws CouldNotPerformException {
        registerRegistryRemote(locationRegistryRemote);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerObserver() throws CouldNotPerformException {
        appClassRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, AppClass, AppClass.Builder>>> source, Map<String, IdentifiableMessage<String, AppClass, AppClass.Builder>> data) -> {
            notifyChange();
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void removeDependencies() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void performInitialConsistencyCheck() throws CouldNotPerformException, InterruptedException {
        try {
            appClassRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
    }

    @Override
    public void shutdown() {
        if (appClassRegistry != null) {
            appClassRegistry.shutdown();
        }
        super.shutdown();
    }

    @Override
    public final void syncDataTypeFlags() throws CouldNotPerformException, InterruptedException {
        setDataField(AppRegistryData.APP_CLASS_REGISTRY_READ_ONLY_FIELD_NUMBER, appClassRegistry.isReadOnly());
        setDataField(AppRegistryData.APP_CLASS_REGISTRY_CONSISTENT_FIELD_NUMBER, appClassRegistry.isConsistent());
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
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

    @Override
    public List<AppConfig> getAppConfigsByAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException {
        return getAppConfigsByAppClassId(appClass.getId());
    }

    @Override
    public List<AppConfig> getAppConfigsByAppClassId(String appClassId) throws CouldNotPerformException, InterruptedException {
        if (!containsAppClassById(appClassId)) {
            throw new NotAvailableException("appClassId [" + appClassId + "]");
        }

        List<AppConfig> appConfigs = new ArrayList<>();
        for (AppConfig appConfig : getAppConfigs()) {
            if (appConfig.getAppClassId().equals(appClassId)) {
                appConfigs.add(appConfig);
            }
        }
        return appConfigs;
    }

    @Override
    public Future<AppClass> registerAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> appClassRegistry.register(appClass));
    }

    @Override
    public Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.contains(appClass);
    }

    @Override
    public Boolean containsAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.contains(appClassId);
    }

    @Override
    public Future<AppClass> updateAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> appClassRegistry.update(appClass));
    }

    @Override
    public Future<AppClass> removeAppClass(AppClass appClass) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> appClassRegistry.remove(appClass));
    }

    @Override
    public AppClass getAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.getMessage(appClassId);
    }

    @Override
    public List<AppClass> getAppClasses() throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.getMessages();
    }

    @Override
    public Boolean isAppClassRegistryReadOnly() throws CouldNotPerformException, InterruptedException {
        return appClassRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAppClassRegistryConsistent() throws CouldNotPerformException {
        return appClassRegistry.isConsistent();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isAppConfigRegistryConsistent() throws CouldNotPerformException {
        return appConfigRegistry.isConsistent();
    }
}
