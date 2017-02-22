package org.openbase.bco.manager.location.core;

/*
 * #%L
 * BCO Manager Location Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.detector.PresenceDetector;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteManager;
import static org.openbase.bco.manager.location.core.LocationManagerController.LOGGER;
import org.openbase.bco.manager.location.lib.LocationController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.action.SnapshotType.Snapshot;
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
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.SmokeStateType;
import rst.domotic.state.StandbyStateType;
import rst.domotic.state.TamperStateType;
import rst.domotic.state.TemperatureStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType;

/**
 *
 * UnitConfig
 */
public class LocationControllerImpl extends AbstractBaseUnitController<LocationData, LocationData.Builder> implements LocationController {

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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionConfigType.ActionConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Snapshot.getDefaultInstance()));
    }

    private final PresenceDetector presenceDetector;
    private final ServiceRemoteManager serviceRemoteManager;

    public LocationControllerImpl() throws InstantiationException {
        super(LocationControllerImpl.class, LocationData.newBuilder());
        this.serviceRemoteManager = new ServiceRemoteManager() {
            @Override
            protected Set<ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException {
                return LocationControllerImpl.this.getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(Observable source, Object data) throws NotAvailableException, InterruptedException {
                updateCurrentState();
            }
        };
        this.presenceDetector = new PresenceDetector();
        this.presenceDetector.addDataObserver(new Observer<PresenceState>() {
            @Override
            public void update(Observable<PresenceState> source, PresenceState data) throws Exception {
                try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                    dataBuilder.getInternalBuilder().setPresenceState(data);
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not apply presense state change!", ex);
                }
            }
        });

//        try {
//            GlobalScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        System.out.println(this + " connnection State: " + getControllerAvailabilityState().name());
//                        LocationRemote l = new LocationRemote();
//                        l.init(scope);
//                        l.activate();
//                        System.out.println("====== New Remote "+ScopeGenerator.generateStringRep(l.getScope())+" =========");
//                        System.out.println(l + " connnection State: " + l.getConnectionState().name());
//                        l.waitForData();
//                        System.out.println(l + " temp State: " + l.getTemperatureState().getTemperature());
//                        System.out.println(l + " connnection State: " + l.getConnectionState().name());
//                        l.shutdown();
//                        System.out.println("====== Pool ================");
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " connnection State: " + Units.getUnitByScope(scope, false).getConnectionState().name());
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " active: " + Units.getUnitByScope(scope, true).isActive());
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " locked: " + Units.getUnitByScope(scope, false).isLocked());
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " data avail: " + Units.getUnitByScope(scope, false).isDataAvailable());
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " temp: " + ((LocationRemote) Units.getUnitByScope(scope, false)).getData().getTemperatureState().getTemperature());
//                        System.out.println("======================");
//                    } catch (Exception ex) {
//                        Logger.getLogger(LocationControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }, 1, 5000, TimeUnit.MILLISECONDS);
//        } catch (Exception ex) {
//            Logger.getLogger(LocationControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        addDataObserver(new Observer<LocationData>() {
//            @Override
//            public void update(Observable<LocationData> source, LocationData data) throws Exception {
//                System.out.println("#### Location observation temp: " + data.getTemperatureState().getTemperature());
//            }
//        });
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        LOGGER.debug("Init location [" + config.getLabel() + "]");
        try {
            Registries.getUnitRegistry().waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
        presenceDetector.init(this);
    }

    @Override
    public synchronized UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        System.out.println("Apply config update for location["+config.getLabel()+"]");
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        System.out.println("Applied super config update!");
        serviceRemoteManager.applyConfigUpdate(unitConfig.getLocationConfig().getUnitIdList());
        System.out.println("ServiceRemoteManager applied config update");
        // if already active than update the current location state.
        if (isActive()) {
            System.out.println("update current status....");
            updateCurrentState();
            System.out.println("updated current status!");
        }
        return unitConfig;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (isActive()) {
            System.out.println("Skipp controller activations because is already active...");
            return;
        }
        LOGGER.debug("Activate location [" + getLabel() + "]!");
        serviceRemoteManager.activate();
        super.activate();
        presenceDetector.activate();
        updateCurrentState();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        LOGGER.debug("Deactivate location [" + getLabel() + "]!");
        super.deactivate();
        serviceRemoteManager.deactivate();
        presenceDetector.deactivate();
    }

    private void updateCurrentState() throws InterruptedException {
        try (ClosableDataBuilder<LocationDataType.LocationData.Builder> dataBuilder = getDataBuilder(this)) {
            for (final ServiceTemplate.ServiceType serviceType : getSupportedServiceTypes()) {

                final Object serviceState;

                try {
                    final AbstractServiceRemote serviceRemote = serviceRemoteManager.getServiceRemote(serviceType);
                    /* When the locationRemote is active and a config update occurs the serviceRemoteManager clears 
                     * its map of service remotes and fills it with new ones. When they are activated an update is triggered while
                     * the map is not completely filled. Therefore the serviceRemote can be null.
                     */
                    if (serviceRemote == null) {
//                        System.out.println("Update for serviceRemote of type["+serviceType.name()+"] because not initialized by serviceRemoteManager");
                        continue;
                    }
                    if (!serviceRemote.isDataAvailable()) {
                        continue;
                    }

                    serviceState = Service.invokeProviderServiceMethod(serviceType, serviceRemote);
                } catch (NotAvailableException ex) {
                    ExceptionPrinter.printHistory("No service data for type[" + serviceType + "] on location available!", ex, LOGGER);
                    continue;
                } catch (NotSupportedException | IllegalArgumentException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException(this, ex), LOGGER);
                    continue;
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update ServiceState[" + serviceType.name() + "] for " + this, ex, LOGGER);
                    continue;
                }

                try {
                    Service.invokeOperationServiceMethod(serviceType, dataBuilder.getInternalBuilder(), serviceState);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new NotSupportedException("Field[" + serviceType.name().toLowerCase().replace("_service", "") + "] is missing in protobuf type " + getDataClass().getSimpleName() + "!", this, ex), LOGGER);
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update current status!", ex), LOGGER, LogLevel.WARN);
        }
    }

    private boolean isServiceTypeSupported(final ServiceType serviceType) throws CouldNotPerformException, InterruptedException {
        if (serviceType == null) {
            assert false;
            throw new NotAvailableException("ServiceType");
        }
        return getSupportedServiceTypes().contains(serviceType);
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }

    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return recordSnapshot(UnitType.UNKNOWN);
    }

    public Future<Snapshot> recordSnapshot(UnitType unitType) throws CouldNotPerformException, InterruptedException {
        try {
            Snapshot.Builder snapshotBuilder = Snapshot.newBuilder();
            Set<UnitRemote> unitRemoteSet = new HashSet<>();

            if (unitType == UnitType.UNKNOWN) {
                // if the type is unknown then take the snapshot for all units
                serviceRemoteManager.getServiceRemoteList().stream().forEach((serviceRemote) -> {
                    unitRemoteSet.addAll(serviceRemote.getInternalUnits());
                });
            } else {
                // for effiecency reasons only one serviceType implemented by the unitType is regarded because the unitRemote is part of
                // every abstractServiceRemotes internal units if the serviceType is implemented by the unitType
                ServiceType serviceType;
                try {
                    serviceType = Registries.getUnitRegistry().getUnitTemplateByType(unitType).getServiceTemplateList().get(0).getType();
                } catch (IndexOutOfBoundsException ex) {
                    // if there is not at least one serviceType for the unitType then the snapshot is empty
                    return CompletableFuture.completedFuture(snapshotBuilder.build());
                }

                for (AbstractServiceRemote abstractServiceRemote : serviceRemoteManager.getServiceRemoteList()) {
                    if (!(serviceType == abstractServiceRemote.getServiceType())) {
                        continue;
                    }

                    Collection<UnitRemote> internalUnits = abstractServiceRemote.getInternalUnits();
                    for (UnitRemote unitRemote : internalUnits) {
                        // just add units with the according type
                        if (unitRemote.getType() == unitType) {
                            unitRemoteSet.add(unitRemote);
                        }
                    }
                }
            }

            // take the snapshot
            for (UnitRemote<?> remote : unitRemoteSet) {
                try {
                    if (UnitProcessor.isDalUnit(remote)) {
                        if (!remote.isConnected()) {
                            throw new NotAvailableException("Unit[" + remote.getLabel() + "] is currently not reachable!");
                        }
                        snapshotBuilder.addAllActionConfig(remote.recordSnapshot().get(2, TimeUnit.SECONDS).getActionConfigList());
                    }
                } catch (ExecutionException | TimeoutException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + remote.getLabel(), ex), LOGGER);
                }
            }
            return CompletableFuture.completedFuture(snapshotBuilder.build());
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        try {
            final Map<String, org.openbase.bco.dal.lib.layer.unit.UnitRemote<?>> unitRemoteMap = new HashMap<>();
            for (AbstractServiceRemote<?, ?> serviceRemote : serviceRemoteManager.getServiceRemoteList()) {
                for (org.openbase.bco.dal.lib.layer.unit.UnitRemote<?> unitRemote : serviceRemote.getInternalUnits()) {
                    unitRemoteMap.put(unitRemote.getId(), unitRemote);
                }
            }

            Collection<Future> futureCollection = new ArrayList<>();
            for (final ActionConfig actionConfig : snapshot.getActionConfigList()) {
                futureCollection.add(unitRemoteMap.get(actionConfig.getUnitId()).applyAction(actionConfig));
            }
            return GlobalCachedExecutorService.allOf(futureCollection, null);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    @Override
    public Future<Void> applyAction(final ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.getServiceRemote(actionConfig.getServiceType()).applyAction(actionConfig);
    }

    @Override
    public ServiceRemote getServiceRemote(final ServiceType serviceType) {
        return serviceRemoteManager.getServiceRemote(serviceType);
    }

    @Override
    public List<String> getNeighborLocationIds() throws CouldNotPerformException {
        List<String> neighborIdList = new ArrayList<>();
        for (UnitConfig locationConfig : LocationManagerController.getInstance().getLocationRegistry().getNeighborLocations(getId())) {
            neighborIdList.add(locationConfig.getId());
        }
        return neighborIdList;
    }
}
