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
import de.citec.jp.JPConnectionConfigDatabaseDirectory;
import de.citec.jp.JPLocationConfigDatabaseDirectory;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.lm.core.consistency.ChildWithSameLabelConsistencyHandler;
import de.citec.lm.core.consistency.LocationLoopConsistencyHandler;
import de.citec.lm.core.consistency.LocationUnitIdConsistencyHandler;
import de.citec.lm.core.consistency.PositionConsistencyHandler;
import de.citec.lm.core.plugin.PublishLocationTransformationRegistryPlugin;
import de.citec.lm.core.registry.dbconvert.LocationConfig_0_To_1_DBConverter;
import de.citec.lm.lib.generator.ConnectionIDGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionConfig.getDefaultInstance()));
    }
    
    private final ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistryType.LocationRegistry.Builder> locationConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, ConnectionConfig, ConnectionConfig.Builder, LocationRegistryType.LocationRegistry.Builder> connectionConfigRegistry;
    
    private final DeviceRegistryRemote deviceRegistryRemote;
    private Observer<DeviceRegistry> deviceRegistryUpdateObserver;
    
    public LocationRegistryService() throws InstantiationException, InterruptedException {
        super(LocationRegistry.newBuilder());
        try {
            locationConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(LocationConfig.class, getBuilderSetup(), getFieldDescriptor(LocationRegistry.LOCATION_CONFIG_FIELD_NUMBER), new LocationIDGenerator(), JPService.getProperty(JPLocationConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            connectionConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(ConnectionConfig.class, getBuilderSetup(), getFieldDescriptor(LocationRegistry.CONNECTION_CONFIG_FIELD_NUMBER), new ConnectionIDGenerator(), JPService.getProperty(JPConnectionConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            
            locationConfigRegistry.activateVersionControl(LocationConfig_0_To_1_DBConverter.class.getPackage());
            connectionConfigRegistry.activateVersionControl(LocationConfig_0_To_1_DBConverter.class.getPackage());
            
            deviceRegistryUpdateObserver = (Observable<DeviceRegistry> source, DeviceRegistry data) -> {
                locationConfigRegistry.checkConsistency();
                connectionConfigRegistry.checkConsistency();
            };
            
            deviceRegistryRemote = new DeviceRegistryRemote();
            
            locationConfigRegistry.loadRegistry();
            connectionConfigRegistry.loadRegistry();
            
            locationConfigRegistry.registerConsistencyHandler(new RootConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new ParentChildConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new ChildWithSameLabelConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new ScopeConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(deviceRegistryRemote));
            locationConfigRegistry.registerConsistencyHandler(new PositionConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationLoopConsistencyHandler());
            locationConfigRegistry.registerPlugin(new PublishLocationTransformationRegistryPlugin());
            
            locationConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>>> source, Map<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>> data) -> {
                notifyChange();
            });
            
            connectionConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>>> source, Map<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>> data) -> {
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

    /**
     * {@inheritDoc}
     *
     * @throws java.lang.InterruptedException {@inheritDoc}
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
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
        
        try {
            connectionConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            logger.warn("Initial consistency check failed!");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.lang.InterruptedException {@inheritDoc}
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        deviceRegistryRemote.removeObserver(deviceRegistryUpdateObserver);
        super.deactivate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        
        if (deviceRegistryRemote != null) {
            deviceRegistryRemote.shutdown();
        }
        
        if (locationConfigRegistry != null) {
            locationConfigRegistry.shutdown();
        }
        
        if (connectionConfigRegistry != null) {
            connectionConfigRegistry.shutdown();
        }
        
        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setField(LocationRegistry.LOCATION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, locationConfigRegistry.isReadOnly());
        // TODO mpohling setup for connection registry!
        super.notifyChange();
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(LocationRegistryInterface.class, this, server);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.register(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig getLocationConfigById(final String locationId) throws CouldNotPerformException {
        return locationConfigRegistry.get(locationId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException {
        getData();
        return locationConfigRegistry.getMessages().stream()
                .filter(m -> m.getLabel().equals(locationLabel))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLabel(final String unitLabel, final String locationId) throws CouldNotPerformException {
        getData();
        return deviceRegistryRemote.getUnitConfigsByLabel(unitLabel).stream()
                .filter(u -> u.getPlacementConfig().getLocationId().equals(locationId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException {
        return locationConfigRegistry.contains(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.contains(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig updateLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.update(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig removeLocationConfig(LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.remove(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException {
        return locationConfigRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final String locationId) throws CouldNotPerformException {
        List<UnitConfigType.UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getLocationConfigById(locationId).getUnitIdList()) {
            unitConfigList.add(deviceRegistryRemote.getUnitConfigById(unitConfigId));
        }
        return unitConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs(final String locationId) throws CouldNotPerformException {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigs(locationId)) {
            serviceConfigList.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws de.citec.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final UnitTemplateType.UnitTemplate.UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfigType.UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfigType.UnitConfig unitConfig;
        
        for (String unitConfigId : getLocationConfigById(locationConfigId).getUnitIdList()) {
            try {
                unitConfig = deviceRegistryRemote.getUnitConfigById(unitConfigId);
                if (unitConfig.getType().equals(type)) {
                    unitConfigList.add(unitConfig);
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not resolve UnitConfigId[" + unitConfigId + "] by device registry!", ex), logger);
            }
        }
        return unitConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws de.citec.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final ServiceTemplateType.ServiceTemplate.ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfigType.UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;
        
        for (String unitConfigId : getLocationConfigById(locationConfigId).getUnitIdList()) {
            try {
                unitConfig = deviceRegistryRemote.getUnitConfigById(unitConfigId);
                for (ServiceConfigType.ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getType().equals(type)) {
                        unitConfigList.add(unitConfig);
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not resolve UnitConfigId[" + unitConfigId + "] by device registry!", ex), logger);
            }
        }
        return unitConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws de.citec.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public LocationConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException {
        for (LocationConfig locationConfig : locationConfigRegistry.getMessages()) {
            if (locationConfig.hasRoot() && locationConfig.getRoot()) {
                return locationConfig;
            }
        }
        throw new NotAvailableException("rootlocation");
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<Boolean> isLocationConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(locationConfigRegistry.isReadOnly());
    }

    public ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> getLocationConfigRegistry() {
        return locationConfigRegistry;
    }
}
