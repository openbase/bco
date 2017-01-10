package org.openbase.bco.registry.location.remote;

/*
 * #%L
 * BCO Registry Location Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.storage.registry.RegistryRemote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;
import rst.domotic.registry.LocationRegistryDataType.LocationRegistryData;
import rst.domotic.unit.UnitProbabilityCollectionType.UnitProbabilityCollection;
import rst.tracking.PointingRay3DFloatCollectionType.PointingRay3DFloatCollection;
import rst.tracking.PointingRay3DFloatType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationRegistryRemote extends AbstractRegistryRemote<LocationRegistryData> implements LocationRegistry, RegistryRemote<LocationRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
    }

    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> locationUnitConfigRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> connectionUnitConfigRemoteRegistry;
    private UnitRegistry unitRegistry;

    public LocationRegistryRemote() throws InstantiationException {
        super(JPLocationRegistryScope.class, LocationRegistryData.class);
        try {
            this.locationUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, LocationRegistryData.LOCATION_UNIT_CONFIG_FIELD_NUMBER);
            this.connectionUnitConfigRemoteRegistry = new SynchronizedRemoteRegistry<>(this, LocationRegistryData.CONNECTION_UNIT_CONFIG_FIELD_NUMBER);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        try {
            this.unitRegistry = CachedUnitRegistryRemote.getRegistry();
        } catch (NotAvailableException ex) {
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
        CachedUnitRegistryRemote.waitForData();
        super.activate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            unitRegistry.shutdown();
        } finally {
            super.shutdown();
        }
    }

    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(locationUnitConfigRemoteRegistry);
        registerRemoteRegistry(connectionUnitConfigRemoteRegistry);
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getLocationConfigRemoteRegistry() {
        return locationUnitConfigRemoteRegistry;
    }

    public SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> getConnectionConfigRemoteRegistry() {
        return connectionUnitConfigRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> registerLocationConfig(final UnitConfig locationConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(locationConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register location config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitConfig getLocationConfigById(final String locationId) throws CouldNotPerformException {
        validateData();
        return locationUnitConfigRemoteRegistry.getMessage(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException {
        validateData();
        return locationUnitConfigRemoteRegistry.getMessages().stream()
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
        return unitRegistry.getUnitConfigsByLabel(unitLabel).stream()
                .filter(u -> u.getPlacementConfig().getLocationId().equals(locationId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfig(final UnitConfig locationConfig) throws CouldNotPerformException {
        validateData();
        return locationUnitConfigRemoteRegistry.contains(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException {
        validateData();
        return locationUnitConfigRemoteRegistry.contains(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> updateLocationConfig(final UnitConfig locationConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(locationConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update location[" + locationConfig + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> removeLocationConfig(final UnitConfig locationConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(locationConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove location[" + locationConfig + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getLocationConfigs() throws CouldNotPerformException, NotAvailableException {
        validateData();
        List<UnitConfig> messages = locationUnitConfigRemoteRegistry.getMessages();
        return messages;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocation(final String locationId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getLocationConfigById(locationId).getLocationConfig().getUnitIdList()) {
            unitConfigList.add(unitRegistry.getUnitConfigById(unitConfigId));
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
        for (UnitConfig location : getLocationConfigsByLabel(locationLabel)) {
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
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByLocation(final UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        UnitConfig unitConfig;

        for (String unitConfigId : getLocationConfigById(locationConfigId).getLocationConfig().getUnitIdList()) {
            try {
                unitConfig = unitRegistry.getUnitConfigById(unitConfigId);
                if (unitConfig.getType().equals(type) || unitRegistry.getSubUnitTypesOfUnitType(type).contains(unitConfig.getType())) {
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
        for (UnitConfig location : getLocationConfigsByLabel(locationLabel)) {
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

        for (String unitConfigId : getLocationConfigById(locationConfigId).getLocationConfig().getUnitIdList()) {
            try {
                unitConfig = unitRegistry.getUnitConfigById(unitConfigId);
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException {
        validateData();
        for (UnitConfig locationConfig : locationUnitConfigRemoteRegistry.getMessages()) {
            if (locationConfig.getLocationConfig().hasRoot() && locationConfig.getLocationConfig().getRoot()) {
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
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getLocationUnitConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> registerConnectionConfig(UnitConfig connectionConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(connectionConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register connection config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitConfig getConnectionConfigById(String connectionId) throws CouldNotPerformException {
        validateData();
        return connectionUnitConfigRemoteRegistry.getMessage(connectionId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getConnectionConfigsByLabel(String connectionLabel) throws CouldNotPerformException {
        validateData();
        return connectionUnitConfigRemoteRegistry.getMessages().stream()
                .filter(m -> m.getLabel().equals(connectionLabel))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsConnectionConfig(UnitConfig connectionConfig) throws CouldNotPerformException {
        validateData();
        return connectionUnitConfigRemoteRegistry.contains(connectionConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsConnectionConfigById(String connectionId) throws CouldNotPerformException {
        validateData();
        return connectionUnitConfigRemoteRegistry.contains(connectionId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> updateConnectionConfig(UnitConfig connectionConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(connectionConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update connection[" + connectionConfig + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitConfig> removeConnectionConfig(UnitConfig connectionConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(connectionConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove connection[" + connectionConfig + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getConnectionConfigs() throws CouldNotPerformException {
        validateData();
        List<UnitConfig> messages = connectionUnitConfigRemoteRegistry.getMessages();
        return messages;
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getUnitConfigsByConnection(String connectionConfigId) throws CouldNotPerformException {
        List<UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getConnectionConfigById(connectionConfigId).getConnectionConfig().getUnitIdList()) {
            unitConfigList.add(unitRegistry.getUnitConfigById(unitConfigId));
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

        for (String unitConfigId : getConnectionConfigById(connectionConfigId).getConnectionConfig().getUnitIdList()) {
            try {
                unitConfig = unitRegistry.getUnitConfigById(unitConfigId);
                if (unitConfig.getType().equals(type) || unitRegistry.getSubUnitTypesOfUnitType(type).contains(unitConfig.getType())) {
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

        for (String unitConfigId : getConnectionConfigById(connectionConfigId).getConnectionConfig().getUnitIdList()) {
            try {
                unitConfig = unitRegistry.getUnitConfigById(unitConfigId);
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
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
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isConnectionConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getConnectionUnitConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UnitConfig> getNeighborLocations(String locationId) throws CouldNotPerformException {
        UnitConfig locationConfig = getLocationConfigById(locationId);
        if (locationConfig.getLocationConfig().getType() != LocationConfig.LocationType.TILE) {
            throw new CouldNotPerformException("Id[" + locationId + "] does not belong to a tile and therefore its neighbors aren't defined!");
        }

        Map<String, UnitConfig> neighborMap = new HashMap<>();
        for (UnitConfig connectionConfig : getConnectionConfigs()) {
            if (connectionConfig.getConnectionConfig().getTileIdList().contains(locationId)) {
                for (String id : connectionConfig.getConnectionConfig().getTileIdList()) {
                    if (id.equals(locationId)) {
                        continue;
                    }

                    neighborMap.put(id, getLocationConfigById(id));
                }
            }
        }

        return new ArrayList<>(neighborMap.values());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isLocationConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getLocationUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isConnectionConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getConnectionUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param pointingRay3DFloat {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitProbabilityCollection> computeUnitIntersection(PointingRay3DFloatType.PointingRay3DFloat pointingRay3DFloat) throws CouldNotPerformException {
        try {
            validateData();
            return RPCHelper.callRemoteMethod(pointingRay3DFloat, this, UnitProbabilityCollection.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not compute unit intersection!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param pointingRay3DFloatCollection {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitProbabilityCollection> computeUnitIntersection(PointingRay3DFloatCollection pointingRay3DFloatCollection) throws CouldNotPerformException {
        try {
            validateData();
            return RPCHelper.callRemoteMethod(pointingRay3DFloatCollection, this, UnitProbabilityCollection.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not compute unit intersection!", ex);
        }
    }
}
