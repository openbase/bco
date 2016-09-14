package org.openbase.bco.registry.location.core;

/*
 * #%L
 * REM LocationRegistry Core
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.location.core.consistency.ChildWithSameLabelConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.ConnectionLabelConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.ConnectionLocationConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.ConnectionScopeConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.ConnectionTilesConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.ConnectionTransformationFrameConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationChildConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationIdConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationLoopConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationParentConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationPlacementConfigConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationPositionConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationScopeConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationTransformationFrameConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.LocationUnitIdConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.RootConsistencyHandler;
import org.openbase.bco.registry.location.core.consistency.RootLocationExistencConsistencyHandler;
import org.openbase.bco.registry.location.core.dbconvert.LocationConfig_0_To_1_DBConverter;
import org.openbase.bco.registry.location.core.plugin.PublishConnectionTransformationRegistryPlugin;
import org.openbase.bco.registry.location.core.plugin.PublishLocationTransformationRegistryPlugin;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.lib.generator.ConnectionIDGenerator;
import org.openbase.bco.registry.location.lib.generator.LocationIDGenerator;
import org.openbase.bco.registry.location.lib.jp.JPConnectionConfigDatabaseDirectory;
import org.openbase.bco.registry.location.lib.jp.JPLocationConfigDatabaseDirectory;
import org.openbase.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.storage.file.ProtoBufJSonFileProvider;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryDataType.LocationRegistryData;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;

/**
 *
 * @author mpohling
 */
public class LocationRegistryController extends RSBCommunicationService<LocationRegistryData, LocationRegistryData.Builder> implements LocationRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionConfig.getDefaultInstance()));
    }

    private final ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistryData.Builder> locationConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, ConnectionConfig, ConnectionConfig.Builder, LocationRegistryData.Builder> connectionConfigRegistry;

    private final DeviceRegistryRemote deviceRegistryRemote;

    public LocationRegistryController() throws InstantiationException, InterruptedException {
        super(LocationRegistryData.newBuilder());
        try {
            locationConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(LocationConfig.class, getBuilderSetup(), getDataFieldDescriptor(LocationRegistryData.LOCATION_CONFIG_FIELD_NUMBER), new LocationIDGenerator(), JPService.getProperty(JPLocationConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());
            connectionConfigRegistry = new ProtoBufFileSynchronizedRegistry<>(ConnectionConfig.class, getBuilderSetup(), getDataFieldDescriptor(LocationRegistryData.CONNECTION_CONFIG_FIELD_NUMBER), new ConnectionIDGenerator(), JPService.getProperty(JPConnectionConfigDatabaseDirectory.class).getValue(), new ProtoBufJSonFileProvider());

            locationConfigRegistry.activateVersionControl(LocationConfig_0_To_1_DBConverter.class.getPackage());
            connectionConfigRegistry.activateVersionControl(LocationConfig_0_To_1_DBConverter.class.getPackage());

            deviceRegistryRemote = new DeviceRegistryRemote();

            locationConfigRegistry.loadRegistry();
            connectionConfigRegistry.loadRegistry();

            locationConfigRegistry.registerConsistencyHandler(new LocationPlacementConfigConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationPositionConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new RootConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationChildConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationIdConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationParentConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new RootLocationExistencConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationLoopConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new ChildWithSameLabelConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationScopeConsistencyHandler());
            locationConfigRegistry.registerConsistencyHandler(new LocationUnitIdConsistencyHandler(deviceRegistryRemote));
            locationConfigRegistry.registerConsistencyHandler(new LocationTransformationFrameConsistencyHandler(locationConfigRegistry));
            locationConfigRegistry.registerPlugin(new PublishLocationTransformationRegistryPlugin());

            connectionConfigRegistry.registerConsistencyHandler(new ConnectionLabelConsistencyHandler());
            connectionConfigRegistry.registerConsistencyHandler(new ConnectionTilesConsistencyHandler(locationConfigRegistry));
            connectionConfigRegistry.registerConsistencyHandler(new ConnectionLocationConsistencyHandler(locationConfigRegistry));
            connectionConfigRegistry.registerConsistencyHandler(new ConnectionScopeConsistencyHandler(locationConfigRegistry));
            connectionConfigRegistry.registerConsistencyHandler(new ConnectionTransformationFrameConsistencyHandler(locationConfigRegistry));
            connectionConfigRegistry.registerPlugin(new PublishConnectionTransformationRegistryPlugin(locationConfigRegistry));

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

    public void init() throws InitializationException, InterruptedException {
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
            deviceRegistryRemote.activate();
            deviceRegistryRemote.waitForData();
            locationConfigRegistry.registerDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
            connectionConfigRegistry.registerDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            locationConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }

        try {
            connectionConfigRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
            notifyChange();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.lang.InterruptedException {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        locationConfigRegistry.removeDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        connectionConfigRegistry.removeDependency(deviceRegistryRemote.getDeviceConfigRemoteRegistry());
        deviceRegistryRemote.deactivate();
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

        deviceRegistryRemote.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public final void notifyChange() throws CouldNotPerformException, InterruptedException {
        // sync read only flags
        setDataField(LocationRegistryData.LOCATION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, locationConfigRegistry.isReadOnly());
        setDataField(LocationRegistryData.CONNECTION_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, connectionConfigRegistry.isReadOnly());
        setDataField(LocationRegistryData.LOCATION_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, locationConfigRegistry.isConsistent());
        setDataField(LocationRegistryData.CONNECTION_CONFIG_REGISTRY_CONSISTENT_FIELD_NUMBER, connectionConfigRegistry.isConsistent());
        super.notifyChange();
    }

    /**
     * {@inheritDoc}
     *
     * @param server
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(LocationRegistry.class, this, server);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<LocationConfig> registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> locationConfigRegistry.register(locationConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig getLocationConfigById(final String locationId) throws CouldNotPerformException {
        return locationConfigRegistry.get(locationId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException {
        return locationConfigRegistry.getMessages().stream()
                .filter(m -> m.getLabel().equalsIgnoreCase(locationLabel))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException {
        return locationConfigRegistry.contains(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return locationConfigRegistry.contains(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<LocationConfig> updateLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> locationConfigRegistry.update(locationConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<LocationConfig> removeLocationConfig(LocationConfig locationConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> locationConfigRegistry.remove(locationConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException {
        return locationConfigRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocation(final UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getLocationConfigById(locationConfigId).getUnitIdList()) {
            try {
                unitConfig = deviceRegistryRemote.getUnitConfigById(unitConfigId);
                if (unitConfig.getType().equals(type) || deviceRegistryRemote.getSubUnitTypesOfUnitType(type).contains(unitConfig.getType())) {
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocation(final ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getLocationConfigById(locationConfigId).getUnitIdList()) {
            try {
                unitConfig = deviceRegistryRemote.getUnitConfigById(unitConfigId);
                for (ServiceConfigType.ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceTemplate().getType().equals(type)) {
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isLocationConfigRegistryReadOnly() throws CouldNotPerformException {
        return locationConfigRegistry.isReadOnly();
    }

    /**
     * Returns the internal location config registry.
     *
     * @return the location config registry.
     */
    public ProtoBufFileSynchronizedRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistryData.Builder> getLocationConfigRegistry() {
        return locationConfigRegistry;
    }

    /**
     * Returns the internal connection config registry.
     *
     * @return the connection config registry.
     */
    public ProtoBufFileSynchronizedRegistry<String, ConnectionConfig, ConnectionConfig.Builder, LocationRegistryData.Builder> getConnectionConfigRegistry() {
        return connectionConfigRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<ConnectionConfig> registerConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> connectionConfigRegistry.register(connectionConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig getConnectionConfigById(String connectionId) throws CouldNotPerformException {
        return connectionConfigRegistry.get(connectionId).getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        return connectionConfigRegistry.contains(connectionConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsConnectionConfigById(String connectionId) throws CouldNotPerformException {
        return connectionConfigRegistry.contains(connectionId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<ConnectionConfig> updateConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> connectionConfigRegistry.update(connectionConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<ConnectionConfig> removeConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        return GlobalExecutionService.submit(() -> connectionConfigRegistry.remove(connectionConfig));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ConnectionConfig> getConnectionConfigs() throws CouldNotPerformException {
        return connectionConfigRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByConnection(UnitType type, String connectionConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getConnectionConfigById(connectionConfigId).getUnitIdList()) {
            try {
                unitConfig = deviceRegistryRemote.getUnitConfigById(unitConfigId);
                if (unitConfig.getType().equals(type) || deviceRegistryRemote.getSubUnitTypesOfUnitType(type).contains(unitConfig.getType())) {
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByConnection(ServiceType type, String connectionConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getConnectionConfigById(connectionConfigId).getUnitIdList()) {
            try {
                unitConfig = deviceRegistryRemote.getUnitConfigById(unitConfigId);
                for (ServiceConfigType.ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceTemplate().getType().equals(type)) {
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isConnectionConfigRegistryReadOnly() throws CouldNotPerformException {
        return connectionConfigRegistry.isReadOnly();
    }

    @Override
    public List<LocationConfig> getNeighborLocations(String locationId) throws CouldNotPerformException {
        LocationConfig locationConfig = getLocationConfigById(locationId);
        if (locationConfig.getType() != LocationConfig.LocationType.TILE) {
            throw new CouldNotPerformException("Id[" + locationId + "] does not belong to a tile and therefore its neighbors aren't defined!");
        }

        Map<String, LocationConfig> neighborMap = new HashMap<>();
        for (ConnectionConfig connectionConfig : getConnectionConfigs()) {
            if (connectionConfig.getTileIdList().contains(locationId)) {
                for (String id : connectionConfig.getTileIdList()) {
                    if (id.equals(locationId)) {
                        continue;
                    }

                    neighborMap.put(id, getLocationConfigById(id));
                }
            }
        }

        return new ArrayList<>(neighborMap.values());
    }
}
