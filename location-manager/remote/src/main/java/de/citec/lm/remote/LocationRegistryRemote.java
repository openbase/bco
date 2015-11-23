/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.remote;

import de.citec.lm.lib.generator.LocationIDGenerator;
import de.citec.jul.storage.registry.RemoteRegistry;
import de.citec.lm.lib.registry.LocationRegistryInterface;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.extension.rsb.scope.ScopeProvider;
import java.util.ArrayList;
import java.util.List;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.LocationConfigType;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class LocationRegistryRemote extends RSBRemoteService<LocationRegistry> implements LocationRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationRemoteRegistry;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public LocationRegistryRemote() throws InstantiationException {
        try {
            this.locationRemoteRegistry = new RemoteRegistry<>(new LocationIDGenerator());
            deviceRegistryRemote = new DeviceRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     *
     * @param label
     * @param location
     * @throws InitializationException {@inheritDoc}
     * @deprecated this method makes no sense in this context and should be removed within next release. TODO mpohling: remove within next release for registry remotes.
     */
    @Override
    public void init(final String label, final ScopeProvider location) throws InitializationException {
        deviceRegistryRemote.init();
        super.init(label, location);
    }

    /**
     * Method initializes the remote with the given scope for the server registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     */
    @Override
    public synchronized void init(final Scope scope) throws InitializationException {
        deviceRegistryRemote.init();
        super.init(scope);
    }

    /**
     * Method initializes the remote with the default registry connection scope.
     *
     * @throws InitializationException {@inheritDoc}
     */
    public void init() throws InitializationException {
        this.init(JPService.getProperty(JPLocationRegistryScope.class).getValue());
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.lang.InterruptedException {@inheritDoc}
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @param data
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void notifyUpdated(final LocationRegistry data) throws CouldNotPerformException {
        locationRemoteRegistry.notifyRegistryUpdated(data.getLocationConfigList());
    }

    public RemoteRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> getLocationRemoteRegistry() {
        return locationRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public LocationConfig getLocationConfigById(final String locationId) throws CouldNotPerformException {
        getData();
        return locationRemoteRegistry.getMessage(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigsByLabel(final String locationLabel) throws CouldNotPerformException {
        getData();
        return locationRemoteRegistry.getMessages().stream()
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
    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        getData();
        return locationRemoteRegistry.contains(locationConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsLocationConfigById(final String locationId) throws CouldNotPerformException {
        getData();
        return locationRemoteRegistry.contains(locationId);
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
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
     * @throws de.citec.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws de.citec.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<LocationConfig> messages = locationRemoteRegistry.getMessages();
        return messages;
    }

    /**
     * {@inheritDoc}
     *
     * @throws de.citec.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final String locationId) throws CouldNotPerformException, NotAvailableException {
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
     * @throws de.citec.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final UnitTemplate.UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
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
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final ServiceType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
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
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs(final String locationId) throws CouldNotPerformException, NotAvailableException {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfigType.UnitConfig unitConfig : getUnitConfigs(locationId)) {
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
    public LocationConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException {
        getData();
        for (LocationConfig locationConfig : locationRemoteRegistry.getMessages()) {
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
        if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
            return CompletableFuture.completedFuture(true);
        }
        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the location config registry!!", ex);
        }
    }
}
