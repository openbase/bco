package org.openbase.bco.dal.remote.unit.location;

import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.layer.unit.location.Location;
import org.openbase.bco.dal.remote.service.ServiceRemoteManager;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import static org.openbase.bco.dal.remote.unit.Units.CONNECTION;
import static org.openbase.bco.dal.remote.unit.Units.LOCATION;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.pattern.Observable;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.AlarmStateType;
import rst.domotic.state.BlindStateType;
import rst.domotic.state.BrightnessStateType;
import rst.domotic.state.ColorStateType;
import rst.domotic.state.MotionStateType;
import rst.domotic.state.PowerConsumptionStateType;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PresenceStateType;
import rst.domotic.state.SmokeStateType;
import rst.domotic.state.StandbyStateType;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.state.TamperStateType;
import rst.domotic.state.TemperatureStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import rst.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationRemote extends AbstractUnitRemote<LocationData> implements Location {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationDataType.LocationData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColorType.HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorStateType.ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorType.Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColorType.RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerStateType.PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmStateType.AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionStateType.MotionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionStateType.PowerConsumptionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindStateType.BlindState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeStateType.SmokeState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyStateType.StandbyState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperStateType.TamperState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessStateType.BrightnessState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureStateType.TemperatureState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceStateType.PresenceState.getDefaultInstance()));
    }

    private final ServiceRemoteManager<LocationData> serviceRemoteManager;

    public LocationRemote() {
        super(LocationData.class);
        this.serviceRemoteManager = new ServiceRemoteManager<LocationData>(this) {
            @Override
            protected Set<ServiceTemplate.ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException {
                return getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(Observable source, Object data) throws NotAvailableException, InterruptedException {
                // anything needed here?
            }
        };
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        serviceRemoteManager.applyConfigUpdate(unitConfig.getLocationConfig().getUnitIdList());
        return unitConfig;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        // TODO is this wait for data realy needed? blocking activation method is some kind of bad behaviour.
        CachedLocationRegistryRemote.waitForData();
        serviceRemoteManager.activate();
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        serviceRemoteManager.deactivate();
        super.deactivate();
    }

    @Override
    public Future<Snapshot> recordSnapshot(UnitType unitType) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(unitType, this, Snapshot.class);
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(snapshot, this, Void.class);
    }

    //TODO release2.0: LocationRemote should use applyActionMethod of AbstractUnitRemote, this should work when every communication service has a transaction id
    @Override
    public Future<ActionFuture> applyAction(ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(updateActionDescription(actionDescription.toBuilder()).build(), this, ActionFuture.class);
    }
    
    protected ActionDescription.Builder updateActionDescription(final ActionDescription.Builder actionDescription) throws CouldNotPerformException {
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        serviceStateDescription.setUnitId(getId());

        actionDescription.setDescription(actionDescription.getDescription().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));
        //TODO: update USER key with authentication, should not be needed anymore in release 2.0 because the location remote
        // should use the applyAction method of the AbstractUnitRemote which does it.
        actionDescription.setLabel(actionDescription.getLabel().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));
        
        return actionDescription;
    }

    @Override
    public ServiceRemote getServiceRemote(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws NotAvailableException {
        return serviceRemoteManager.getServiceRemote(serviceType);
    }

    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated please use Registries.getLocationRegistry().getNeighborLocations(String locationId) instead.
     */
    @Override
    @Deprecated
    public List<String> getNeighborLocationIds() throws CouldNotPerformException {
        final List<String> neighborIdList = new ArrayList<>();
        try {
            for (UnitConfig locationConfig : CachedLocationRegistryRemote.getRegistry().getNeighborLocations(getId())) {
                neighborIdList.add(locationConfig.getId());
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get CachedLocationRegistryRemote!", ex);
        }
        return neighborIdList;
    }

    // TODO: move into interface as default implementation
    public List<LocationRemote> getNeighborLocationList(final boolean waitForData) throws CouldNotPerformException {
        final List<LocationRemote> neighborList = new ArrayList<>();
        try {
            for (UnitConfig locationUnitConfig : CachedLocationRegistryRemote.getRegistry().getNeighborLocations(getId())) {
                neighborList.add(Units.getUnit(locationUnitConfig, waitForData, LOCATION));
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get all neighbors!", ex);
        }
        return neighborList;
    }

    public List<LocationRemote> getChildLocationList(final boolean waitForData) throws CouldNotPerformException {
        final List<LocationRemote> childList = new ArrayList<>();
        for (String childId : getConfig().getLocationConfig().getChildIdList()) {
            try {
                childList.add(Units.getUnit(CachedLocationRegistryRemote.getRegistry().getLocationConfigById(childId), waitForData, LOCATION));
            } catch (InterruptedException ex) {
                throw new CouldNotPerformException("Could not get all child locations!", ex);
            }
        }
        return childList;
    }

    public List<ConnectionRemote> getConnectionList(final boolean waitForData) throws CouldNotPerformException {
        if (!getConfig().getLocationConfig().getType().equals(LocationType.TILE)) {
            throw new CouldNotPerformException("Location is not a Tile!");
        }

        List<ConnectionRemote> connectionList = new ArrayList<>();
        try {
            for (UnitConfig connectionUnitConfig : CachedLocationRegistryRemote.getRegistry().getConnectionConfigs()) {
                ConnectionRemote connection = Units.getUnit(connectionUnitConfig, waitForData, CONNECTION);
                if (connection.getConfig().getConnectionConfig().getTileIdList().contains(getId())) {
                    connectionList.add(connection);
                }
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get all connections!", ex);
        }
        return connectionList;
    }

    /**
     * Method collects all connections between this and the given location and returns those instances.
     *
     * @param locationID the location id of the location to check.
     * @param waitForData flag defines if the method should block until all needed instances are available.
     * @return a collection of unit connection remotes.
     * @throws CouldNotPerformException is thrown if the check could not be performed e.g. if some data is not available yet.
     */
    public List<ConnectionRemote> getDirectConnectionList(final String locationID, final boolean waitForData) throws CouldNotPerformException {
        if (!getConfig().getLocationConfig().getType().equals(LocationType.TILE)) {
            throw new CouldNotPerformException("Location is not a Tile!");
        }

        List<ConnectionRemote> connectionList = new ArrayList<>();
        try {
            for (UnitConfig connectionUnitConfig : Registries.getLocationRegistry().getConnectionConfigs()) {
                ConnectionRemote connection = Units.getUnit(connectionUnitConfig, waitForData, CONNECTION);
                if (connectionUnitConfig.getConnectionConfig().getTileIdList().contains(getId()) && connectionUnitConfig.getConnectionConfig().getTileIdList().contains(locationID)) {
                    connectionList.add(connection);
                }
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get all connections!", ex);
        }
        return connectionList;
    }

    /**
     * Method checks if an direct connection exists between this and the given location.
     *
     * @param locationID the location id of the location to check.
     * @param connectionType the type of the connection. To disable this filter use ConnectionType.UNKNOWN
     * @param waitForData flag defines if the method should block until all needed instances are available.
     * @return true if the specified connection exists.
     * @throws CouldNotPerformException is thrown if the check could not be performed e.g. if some data is not available yet.
     */
    public boolean hasDirectConnection(final String locationID, final ConnectionType connectionType, final boolean waitForData) throws CouldNotPerformException {
        // todo do not interrate over instances if configs providing all needed informations.
        for (ConnectionRemote relatedConnection : getDirectConnectionList(locationID, true)) {
            if (relatedConnection.getConfig().getConnectionConfig().getType().equals(connectionType)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return
     * @throws NotAvailableException
     * @throws InterruptedException
     * @deprecated please use getUnitMap() instead.
     */
    @Deprecated
    public Map<UnitType, List<UnitRemote>> getProvidedUnitMap() throws NotAvailableException, InterruptedException {
        return getUnitMap();
    }

    /**
     * Returns a Map of all units which directly or recursively provided by this location..
     *
     * @return the Map of provided units sorted by their UnitType.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the map is not available.
     * @throws java.lang.InterruptedException is thrown if the current thread was externally interrupted.
     */
    // TODO: move into interface as default implementation
    public Map<UnitType, List<UnitRemote>> getUnitMap() throws NotAvailableException, InterruptedException {
        return getUnitMap(true);
    }

    /**
     * Returns a Map of all units which are directly or recursively provided by this location..
     *
     * @param recursive defines if recursive related unit should be included as well.
     * @return the Map of provided units sorted by their UnitType.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the map is not available.
     * @throws java.lang.InterruptedException is thrown if the current thread was externally interrupted.
     */
    // TODO: move into interface as default implementation
    public Map<UnitType, List<UnitRemote>> getUnitMap(final boolean recursive) throws NotAvailableException, InterruptedException {
        try {
            final Map<UnitType, List<UnitRemote>> unitRemoteMap = new TreeMap<>();
            MultiException.ExceptionStack exceptionStack = null;

            for (final String unitId : getConfig().getLocationConfig().getUnitIdList()) {
                try {
                    UnitRemote<? extends GeneratedMessage> unitRemote = Units.getUnit(unitId, false);

                    // filter recursive units if needed.
                    if (!recursive && !unitRemote.getConfig().getPlacementConfig().getLocationId().equals(getId())) {
                        continue;
                    }

                    if (!unitRemoteMap.containsKey(unitRemote.getUnitType())) {
                        unitRemoteMap.put(unitRemote.getUnitType(), new ArrayList<>());
                    }
                    unitRemoteMap.get(unitRemote.getUnitType()).add(unitRemote);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            try {
                MultiException.checkAndThrow("Could not collect all unit remotes of " + this, exceptionStack);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
            }
            return unitRemoteMap;

        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit map of " + this, ex);
        }
    }

    /**
     *
     * Method returns a list of all units filtered by the given unit type which are directly or recursively provided by this location.
     *
     * @param <UR> the unit remote class type.
     * @param unitType the unit type.
     * @param waitForData if this flag is set to true the current thread will block until all unit remotes are fully synchronized with the unit controllers.
     * @param unitRemoteClass the unit remote class.
     * @return a map of instance of the given remote class.
     * @throws CouldNotPerformException is thrown in case something went wrong.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     */
    // TODO: move into interface as default implementation
    public <UR extends UnitRemote<?>> Collection<UR> getUnits(final UnitType unitType, final boolean waitForData, final Class<UR> unitRemoteClass) throws CouldNotPerformException, InterruptedException {
        return getUnits(unitType, waitForData, unitRemoteClass, true);
    }

    /**
     *
     * Method returns a list of all units filtered by the given unit type which are directly provided by this location.
     * In case the {@code recursive} flag is set to true than recursive related units are included as well.
     *
     * @param <UR> the unit remote class type.
     * @param unitType the unit type.
     * @param waitForData if this flag is set to true the current thread will block until all unit remotes are fully synchronized with the unit controllers.
     * @param unitRemoteClass the unit remote class.
     * @param recursive defines if recursive related unit should be included as well.
     * @return a map of instance of the given remote class.
     * @throws CouldNotPerformException is thrown in case something went wrong.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     */
    // TODO: move into interface as default implementation
    public <UR extends UnitRemote<?>> Collection<UR> getUnits(final UnitType unitType, final boolean waitForData, final Class<UR> unitRemoteClass, final boolean recursive) throws CouldNotPerformException, InterruptedException {
        final List<UR> unitRemote = new ArrayList<>();
        MultiException.ExceptionStack exceptionStack = null;
        Registries.waitForData();
        for (final UnitConfig unitConfig : Registries.getLocationRegistry().getUnitConfigsByLocation(unitType, getId())) {
            try {
                if (recursive || unitConfig.getPlacementConfig().getLocationId().equals(getId())) {
                    unitRemote.add(Units.getUnit(unitConfig, waitForData, unitRemoteClass));
                }
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        try {
            MultiException.checkAndThrow("Could not collect all unit remotes of " + this, exceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }
        return unitRemote;
    }

    /**
     * Method returns the unit template of this unit containing all provided service templates.
     *
     * @param onlyAvailableServices if the filter flag is set to true, only service templates are included which are available for the current instance.
     * @return the {@code UnitTemplate} of this unit.
     * @throws NotAvailableException is thrown if the {@code UnitTemplate} is currently not available.
     */
    public UnitTemplate getTemplate(final boolean onlyAvailableServices) throws NotAvailableException {

        // todo: move this method to the unit interface or at least to the MultiUnitServiceFusion interface shared by the location and group units.
        // return the unfiltered unit template if filter is not active.
        if (!onlyAvailableServices) {
            return super.getUnitTemplate();
        }
        final UnitTemplate.Builder unitTemplateBuilder = super.getUnitTemplate().toBuilder();

        unitTemplateBuilder.clearServiceDescription();
        for (final ServiceDescription serviceDescription : super.getUnitTemplate().getServiceDescriptionList()) {
            if (serviceRemoteManager.isServiceAvailable(serviceDescription.getType())) {
                unitTemplateBuilder.addServiceDescription(serviceDescription);
            }
        }
        return unitTemplateBuilder.build();
    }

    /**
     * Method returns a set of all currently available service types of this unit instance.
     *
     * @return a set of {@code ServiceTypes}.
     * @throws NotAvailableException is thrown if the service types can not be detected.
     */
    public Set<ServiceType> getAvailableServiceTypes() throws NotAvailableException {

        // todo: move this method to the unit interface or at least to the MultiUnitServiceFusion interface shared by the location and group units.
        final Set<ServiceType> serviceTypeList = new HashSet<>();

        for (final ServiceDescription serviceDescription : getTemplate(true).getServiceDescriptionList()) {
            serviceTypeList.add(serviceDescription.getType());
        }
        return serviceTypeList;
    }

    /**
     * Method returns a set of all currently available service descriptions of this unit instance.
     *
     * @return a set of {@code ServiceDescription}.
     * @throws NotAvailableException is thrown if the service types can not be detected.
     */
    public Set<ServiceDescription> getAvailableServiceDescriptions() throws NotAvailableException {

        // todo: move this method to the unit interface or at least to the MultiUnitServiceFusion interface shared by the location and group units.
        final Set<ServiceDescription> serviceDescriptionList = new HashSet<>();

        for (final ServiceDescription serviceDescription : getTemplate(true).getServiceDescriptionList()) {
            serviceDescriptionList.add(serviceDescription);
        }
        return serviceDescriptionList;
    }

    @Override
    public Future<ActionFuture> setStandbyState(final StandbyState standbyState) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        try {
            return applyAction(updateActionDescription(actionDescription, standbyState, ServiceType.STANDBY_STATE_SERVICE).build());
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Interrupted while setting StandbyState.", ex);
        }
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        try {
            return this.getData().getStandbyState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("StandbyState", ex);
        }
    }
}
