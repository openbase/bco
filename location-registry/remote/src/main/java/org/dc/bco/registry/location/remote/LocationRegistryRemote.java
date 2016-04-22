/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.location.remote;

/*
 * #%L
 * REM LocationRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.dc.jul.pattern.Remote;
import org.dc.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class LocationRegistryRemote extends RSBRemoteService<LocationRegistry> implements org.dc.bco.registry.location.lib.LocationRegistry, Remote<LocationRegistry> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationConfigRemoteRegistry;
    private final RemoteRegistry<String, ConnectionConfig, ConnectionConfig.Builder, LocationRegistry.Builder> connectionConfigRemoteRegistry;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public LocationRegistryRemote() throws InstantiationException {
        try {
            this.locationConfigRemoteRegistry = new RemoteRegistry<>();
            this.connectionConfigRemoteRegistry = new RemoteRegistry<>();
            deviceRegistryRemote = new DeviceRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(final Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotTransformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public synchronized void init(final ScopeType.Scope scope) throws InitializationException, InterruptedException {
        deviceRegistryRemote.init();
        super.init(scope);
    }

    /**
     * Method initializes the remote with the default registry connection scope.
     *
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    public void init() throws InitializationException, InterruptedException {
        try {
            this.init(JPService.getProperty(JPLocationRegistryScope.class).getValue());
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
        deviceRegistryRemote.activate();
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        deviceRegistryRemote.shutdown();
        super.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @param data {@inheritDoc}
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void notifyUpdated(final LocationRegistry data) throws CouldNotPerformException {
        locationConfigRemoteRegistry.notifyRegistryUpdated(data.getLocationConfigList());
        connectionConfigRemoteRegistry.notifyRegistryUpdated(data.getConnectionConfigList());
    }

    public RemoteRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> getLocationConfigRemoteRegistry() {
        return locationConfigRemoteRegistry;
    }

    public RemoteRegistry<String, ConnectionConfig, ConnectionConfig.Builder, LocationRegistry.Builder> getConnectionConfigRemoteRegistry() {
        return connectionConfigRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        try {
            return (LocationConfigType.LocationConfig) callMethod("registerLocationConfig", locationConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register location config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig getLocationConfigById(final String locationId) throws CouldNotPerformException {
        getData();
        return locationConfigRemoteRegistry.getMessage(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException {
        getData();
        return locationConfigRemoteRegistry.getMessages().stream()
                .filter(m -> m.getLabel().equalsIgnoreCase(locationLabel))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLabelAndLocation(final String unitLabel, final String locationId) throws CouldNotPerformException {
        getData();
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
    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        getData();
        return locationConfigRemoteRegistry.contains(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException {
        getData();
        return locationConfigRemoteRegistry.contains(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig updateLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        try {
            return (LocationConfigType.LocationConfig) callMethod("updateLocationConfig", locationConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update location[" + locationConfig + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig removeLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        try {
            return (LocationConfigType.LocationConfig) callMethod("removeLocationConfig", locationConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove location[" + locationConfig + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<LocationConfig> messages = locationConfigRemoteRegistry.getMessages();
        return messages;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocation(final String locationId) throws CouldNotPerformException, NotAvailableException {
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
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
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
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
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
    public List<ServiceConfig> getServiceConfigsByLocation(final String locationId) throws CouldNotPerformException, NotAvailableException {
        List<ServiceConfig> serviceConfigList = new ArrayList<>();
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
    public LocationConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException {
        getData();
        for (LocationConfig locationConfig : locationConfigRemoteRegistry.getMessages()) {
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
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the location config registry!!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig registerConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        try {
            return (ConnectionConfig) callMethod("registerConnectionConfig", connectionConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register connection config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig getConnectionConfigById(String connectionId) throws CouldNotPerformException {
        getData();
        return connectionConfigRemoteRegistry.getMessage(connectionId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ConnectionConfig> getConnectionConfigsByLabel(String connectionLabel) throws CouldNotPerformException {
        getData();
        return connectionConfigRemoteRegistry.getMessages().stream()
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
        getData();
        return connectionConfigRemoteRegistry.contains(connectionConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsConnectionConfigById(String connectionId) throws CouldNotPerformException {
        getData();
        return connectionConfigRemoteRegistry.contains(connectionId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig updateConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        try {
            return (ConnectionConfig) callMethod("updateConnectionConfig", connectionConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update connection[" + connectionConfig + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ConnectionConfig removeConnectionConfig(ConnectionConfig connectionConfig) throws CouldNotPerformException {
        try {
            return (ConnectionConfig) callMethod("removeConnectionConfig", connectionConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove connection[" + connectionConfig + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<ConnectionConfig> getConnectionConfigs() throws CouldNotPerformException {
        getData();
        List<ConnectionConfig> messages = connectionConfigRemoteRegistry.getMessages();
        return messages;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.dc.jul.exception.NotAvailableException {@inheritDoc}
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
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
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
    public List<ServiceConfig> getServiceConfigsByConnection(String connectionConfigId) throws CouldNotPerformException {
        List<ServiceConfig> serviceConfigList = new ArrayList<>();
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
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return CompletableFuture.completedFuture(true);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }
        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the location config registry!!", ex);
        }
    }
}
