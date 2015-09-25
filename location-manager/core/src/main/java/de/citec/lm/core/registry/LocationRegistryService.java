/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.registry;

import de.citec.lm.lib.generator.LocationIDGenerator;
import de.citec.lm.lib.registry.LocationRegistryInterface;
import de.citec.lm.core.consistency.ParentChildConsistencyHandler;
import de.citec.lm.core.consistency.RootConsistencyHandler;
import de.citec.lm.core.consistency.ScopeConsistencyHandler;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPLocationConfigDatabaseDirectory;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.lm.core.consistency.LocationLoopConsistencyHandler;
import de.citec.lm.core.consistency.LocationUnitIdConsistencyHandler;
import de.citec.lm.core.consistency.PositionConsistencyHandler;
import de.citec.lm.core.consistency.TransformationConsistencyHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryType;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class LocationRegistryService extends RSBCommunicationService<LocationRegistry, LocationRegistry.Builder> implements LocationRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfigType.LocationConfig.getDefaultInstance()));
    }

    private final ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistryType.LocationRegistry.Builder> locationConfigRegistry;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private Observer<DeviceRegistry> deviceRegistryUpdateObserver;

    public LocationRegistryService() throws InstantiationException, InterruptedException {
        super(LocationRegistry.newBuilder());
        try {
            locationConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(LocationConfig.class, getBuilderSetup(), getFieldDescriptor(LocationRegistry.LOCATION_CONFIG_FIELD_NUMBER), new LocationIDGenerator(), JPService.getProperty(JPLocationConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());

            deviceRegistryUpdateObserver = (Observable<DeviceRegistry> source, DeviceRegistry data) -> {
                locationConfigRegistry.checkConsistency();
            };

            deviceRegistryRemote = new DeviceRegistryRemote();

            locationConfigRegistry.loadRegistry();

            locationConfigRegistry.registerConsistencyHandler(new RootConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new ParentChildConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new ScopeConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(deviceRegistryRemote));
            locationConfigRegistry.registerConsistencyHandler(new PositionConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new TransformationConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationLoopConsistencyHandler());
            locationConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>>> source, Map<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>> data) -> {
                notifyChange();
            });

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        super.init(JPService.getProperty(JPLocationRegistryScope.class).getValue());
        deviceRegistryRemote.init();
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            deviceRegistryRemote.activate();
            deviceRegistryRemote.addObserver(deviceRegistryUpdateObserver);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            locationConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            logger.warn("Initial consistency check failed!");
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        deviceRegistryRemote.removeObserver(deviceRegistryUpdateObserver);
        super.deactivate();
    }

    @Override
    public void shutdown() {

        if (deviceRegistryRemote != null) {
            deviceRegistryRemote.shutdown();
        }

        if (locationConfigRegistry != null) {
            locationConfigRegistry.shutdown();
        }

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(logger, ex);
        }
    }

    @Override
    public void registerMethods(RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(LocationRegistryInterface.class, this, server);
    }

    @Override
    public LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.register(locationConfig);
    }

    @Override
    public LocationConfig getLocationConfigById(String locationConfigId) throws CouldNotPerformException {
        return locationConfigRegistry.get(locationConfigId).getMessage();
    }

    @Override
    public Boolean containsLocationConfigById(String locationConfigId) throws CouldNotPerformException {
        return locationConfigRegistry.contains(locationConfigId);
    }

    @Override
    public Boolean containsLocationConfig(LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.contains(locationConfig);
    }

    @Override
    public LocationConfig updateLocationConfig(LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.update(locationConfig);
    }

    @Override
    public LocationConfig removeLocationConfig(LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.remove(locationConfig);
    }

    @Override
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException {
        return locationConfigRegistry.getMessages();
    }

    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final String locationConfigId) throws CouldNotPerformException {
        List<UnitConfigType.UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getLocationConfigById(locationConfigId).getUnitIdList()) {
            unitConfigList.add(deviceRegistryRemote.getUnitConfigById(unitConfigId));
        }
        return unitConfigList;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs(final String locationConfigId) throws CouldNotPerformException {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs(locationConfigId)) {
            serviceConfigList.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigList;
    }

    @Override
    public Future<Boolean> isLocationConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(locationConfigRegistry.isReadOnly());
    }
}
