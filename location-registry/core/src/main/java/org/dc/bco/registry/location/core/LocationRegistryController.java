/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.core;

import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.bco.registry.location.lib.jp.JPConnectionConfigDatabaseDirectory;
import org.dc.bco.registry.location.lib.jp.JPLocationConfigDatabaseDirectory;
import org.dc.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.storage.file.ProtoBufJSonFileProvider;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.bco.registry.location.core.consistency.ChildWithSameLabelConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.LocationLoopConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.LocationUnitIdConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.ParentChildConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.PositionConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.RootConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.LocationScopeConsistencyHandler;
import org.dc.bco.registry.location.core.plugin.PublishLocationTransformationRegistryPlugin;
import org.dc.bco.registry.location.core.dbconvert.LocationConfig_0_To_1_DBConverter;
import org.dc.bco.registry.location.lib.generator.ConnectionIDGenerator;
import org.dc.bco.registry.location.lib.generator.LocationIDGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.dc.bco.registry.location.core.consistency.ConnectionLabelConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.ConnectionLocationConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.ConnectionScopeConsistencyHandler;
import org.dc.bco.registry.location.core.consistency.ConnectionTilesConsistencyHandler;
import org.dc.bco.registry.location.core.plugin.PublishConnectionTransformationRegistryPlugin;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class LocationRegistryController extends RSBCommunicationService<LocationRegistry, LocationRegistry.Builder> implements org.dc.bco.registry.location.lib.LocationRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionConfig.getDefaultInstance()));
    }

    private final ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, ConnectionConfig, ConnectionConfig.Builder, LocationRegistry.Builder> connectionConfigRegistry;

    private final DeviceRegistryRemote deviceRegistryRemote;
    private Observer<DeviceRegistry> deviceRegistryUpdateObserver;

    public LocationRegistryController() throws InstantiationException, InterruptedException {
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
            locationConfigRegistry.registerConsistencyHandler(new LocationScopeConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(deviceRegistryRemote));
            locationConfigRegistry.registerConsistencyHandler(new PositionConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationLoopConsistencyHandler());
            locationConfigRegistry.registerPlugin(new PublishLocationTransformationRegistryPlugin());

            connectionConfigRegistry.registerConsistencyHandler(new ConnectionLabelConsistencyHandler());
            connectionConfigRegistry.registerConsistencyHandler(new ConnectionTilesConsistencyHandler(locationConfigRegistry));
            connectionConfigRegistry.registerConsistencyHandler(new ConnectionLocationConsistencyHandler(locationConfigRegistry));
            connectionConfigRegistry.registerConsistencyHandler(new ConnectionScopeConsistencyHandler(locationConfigRegistry));
            connectionConfigRegistry.registerPlugin(new PublishConnectionTransformationRegistryPlugin());

            locationConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>>> source, Map<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>> data) -> {
                notifyChange();
            });

            connectionConfigRegistry.addObserver((Observable<Map<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>>> source, Map<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>> data) -> {
                notifyChange();
            });
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
            super.init(JPService.getProperty(JPLocationRegistryScope.class).getValue());
            deviceRegistryRemote.init();
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.lang.InterruptedException {@inheritDoc}
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
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
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }

        try {
            connectionConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.lang.InterruptedException {@inheritDoc}
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setField(LocationRegistry.LOCATION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, locationConfigRegistry.isReadOnly());
        setField(LocationRegistry.CONNECTION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, connectionConfigRegistry.isReadOnly());
        super.notifyChange();
    }

    /**
     * {@inheritDoc}
     *
     * @param server
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(org.dc.bco.registry.location.lib.LocationRegistry.class, this, server);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.register(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig getLocationConfigById(final String locationId) throws CouldNotPerformException {
        return locationConfigRegistry.get(locationId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException {
        return locationConfigRegistry.getMessages().stream()
                .filter(m -> m.getLabel().equals(locationLabel))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLabelAndLocation(final String unitLabel, final String locationId) throws CouldNotPerformException {
        return deviceRegistryRemote.getUnitConfigsByLabel(unitLabel).stream()
                .filter(u -> u.getPlacementConfig().getLocationId().equals(locationId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException {
        return locationConfigRegistry.contains(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.contains(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig updateLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.update(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig removeLocationConfig(LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.remove(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException {
        return locationConfigRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocation(final String locationId) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getLocationConfigById(locationId).getUnitIdList()) {
            unitConfigList.add(deviceRegistryRemote.getUnitConfigById(unitConfigId));
        }
        return unitConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocationLabel(final String locationLabel) throws CouldNotPerformException {
        HashMap<String, UnitConfig> unitConfigMap = new HashMap<>();
        for (LocationConfig location : getLocationConfigsByLabel(locationLabel)) {
            for (UnitConfig unitConfig : getUnitConfigsByLocation(location.getId())) {
                unitConfigMap.put(unitConfig.getId(), unitConfig);
            }
        }
        return new ArrayList<>(unitConfigMap.values());
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigsByLocation(final String locationId) throws CouldNotPerformException {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigsByLocation(locationId)) {
            serviceConfigList.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocation(final UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

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
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocationLabel(final UnitType unitType, final String locationLabel) throws CouldNotPerformException {
        HashMap<String, UnitConfig> unitConfigMap = new HashMap<>();
        for (LocationConfig location : getLocationConfigsByLabel(locationLabel)) {
            for (UnitConfig unitConfig : getUnitConfigsByLocation(unitType, location.getId())) {
                unitConfigMap.put(unitConfig.getId(), unitConfig);
            }
        }
        return new ArrayList<>(unitConfigMap.values());
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocation(final ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
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
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
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
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<Boolean> isLocationConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(locationConfigRegistry.isReadOnly());
    }

    public ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> getLocationConfigRegistry() {
        return locationConfigRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig registerConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        return connectionConfigRegistry.register(connectionConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig getConnectionConfigById(String connectionId) throws CouldNotPerformException {
        return connectionConfigRegistry.get(connectionId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ConnectionConfig> getConnectionConfigsByLabel(String connectionLabel) throws CouldNotPerformException {
        return connectionConfigRegistry.getMessages().stream()
                .filter(m -> m.getLabel().equals(connectionLabel))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        return connectionConfigRegistry.contains(connectionConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsConnectionConfigById(String connectionId) throws CouldNotPerformException {
        return connectionConfigRegistry.contains(connectionId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig updateConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        return connectionConfigRegistry.update(connectionConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig removeConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        return connectionConfigRegistry.remove(connectionConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ConnectionConfig> getConnectionConfigs() throws CouldNotPerformException {
        return connectionConfigRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByConnection(String connectionConfigId) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getConnectionConfigById(connectionConfigId).getUnitIdList()) {
            unitConfigList.add(deviceRegistryRemote.getUnitConfigById(unitConfigId));
        }
        return unitConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByConnection(UnitType type, String connectionConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getConnectionConfigById(connectionConfigId).getUnitIdList()) {
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
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByConnection(ServiceType type, String connectionConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getConnectionConfigById(connectionConfigId).getUnitIdList()) {
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
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigsByConnection(String connectionConfigId) throws CouldNotPerformException {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfig unitConfig : getUnitConfigsByConnection(connectionConfigId)) {
            serviceConfigList.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigList;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<Boolean> isConnectionConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(connectionConfigRegistry.isReadOnly());
    }
}
