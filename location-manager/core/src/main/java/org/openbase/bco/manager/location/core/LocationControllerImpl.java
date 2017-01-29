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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.unit.UnitProcessor;
import org.openbase.bco.dal.lib.layer.unit.location.Location;
import org.openbase.bco.dal.remote.detector.PresenceDetector;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.BlindStateServiceRemote;
import org.openbase.bco.dal.remote.service.BrightnessStateServiceRemote;
import org.openbase.bco.dal.remote.service.ColorStateServiceRemote;
import org.openbase.bco.dal.remote.service.MotionStateServiceRemote;
import org.openbase.bco.dal.remote.service.PowerConsumptionStateServiceRemote;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactory;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactoryImpl;
import org.openbase.bco.dal.remote.service.SmokeAlarmStateServiceRemote;
import org.openbase.bco.dal.remote.service.SmokeStateServiceRemote;
import org.openbase.bco.dal.remote.service.StandbyStateServiceRemote;
import org.openbase.bco.dal.remote.service.TamperStateServiceRemote;
import org.openbase.bco.dal.remote.service.TargetTemperatureStateServiceRemote;
import org.openbase.bco.dal.remote.service.TemperatureStateServiceRemote;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.location.lib.LocationController;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.SmokeStateType.SmokeState;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.state.TamperStateType.TamperState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;

/**
 *
 * UnitConfig
 */
public class LocationControllerImpl extends AbstractConfigurableController<LocationData, LocationData.Builder, UnitConfig> implements LocationController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Snapshot.getDefaultInstance()));
    }

    private final SyncObject serviceRemoteMapLock = new SyncObject("ServiceRemoteMapLock");

    private final PresenceDetector presenceDetector;
    private final Map<ServiceType, AbstractServiceRemote> serviceRemoteMap;
    private final ServiceRemoteFactory serviceRemoteFactory;
    private final Observer serviceDataObserver;

    public LocationControllerImpl() throws InstantiationException {
        super(LocationData.newBuilder());
        this.serviceRemoteMap = new HashMap<>();
        serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();

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

        serviceDataObserver = new Observer() {
            @Override
            public void update(Observable source, Object data) throws Exception {
                updateCurrentState();
            }
        };

//        try {
//            GlobalScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        System.out.println(this + " connnection State: " + getControllerAvailabilityState().name());
//                        LocationRemote l = new LocationRemote();
//                        l.init(scope);
//                        l.activate();
//                        System.out.println("====== New Remote =========");
//                        System.out.println(l + " connnection State: " + l.getConnectionState().name());
//                        l.waitForData();
//                        System.out.println(l + " connnection State: " + l.getConnectionState().name());
//                        l.shutdown();
//                        System.out.println("====== Pool ================");
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " connnection State: " + Units.getUnitByScope(scope, false).getConnectionState().name());
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " active: " + Units.getUnitByScope(scope, false).isActive());
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " locked: " + Units.getUnitByScope(scope, false).isLocked());
//                        System.out.println(Units.getUnitByScope(scope, false).getConnectionState() + " data avail: " + Units.getUnitByScope(scope, false).isDataAvailable());
//                        System.out.println("======================");
//                    } catch (Exception ex) {
//                        Logger.getLogger(LocationControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }, 1, 5000, TimeUnit.MILLISECONDS);
//        } catch (Exception ex) {
//            Logger.getLogger(LocationControllerImpl.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        logger.debug("Init location [" + config.getLabel() + "]");
        try {
            Registries.getUnitRegistry().waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
        presenceDetector.init(this);
    }

    @Override
    public synchronized UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        config = super.applyConfigUpdate(config);
        synchronized (serviceRemoteMapLock) {
            // shutdown all existing instances.
            for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
                remote.shutdown();
            }
            serviceRemoteMap.clear();

            // init a new set for each supported service type.
            Map<ServiceType, Set<UnitConfig>> serviceMap = new HashMap<>();
            for (ServiceType serviceType : getSupportedServiceTypes()) {
                serviceMap.put(serviceType, new HashSet<>());
            }

            // init service unit map
            for (final String unitId : config.getLocationConfig().getUnitIdList()) {

                // resolve unit config by unit registry
                final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);

                // filter non dal units
                if (!UnitConfigProcessor.isDalUnit(unitConfig)) {
                    continue;
                }

                // sort dal unit by service type
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {

                    // filter all services which are not supported by locations.
                    if (!serviceMap.containsKey(serviceConfig.getServiceTemplate().getType())) {
                        continue;
                    }

                    // register unit for service type. UnitConfigs are may added twice because of dublicated type of different service pattern but are filtered by the set. 
                    serviceMap.get(serviceConfig.getServiceTemplate().getType()).add(unitConfig);
                }
            }

            // initialize service remotes
            for (ServiceType serviceType : getSupportedServiceTypes()) {
                final AbstractServiceRemote serviceRemote = serviceRemoteFactory.newInitializedInstance(serviceType, serviceMap.get(serviceType));
                serviceRemoteMap.put(serviceType, serviceRemote);

                // if already active than update the current location state.
                if (isActive()) {
                    serviceRemote.activate();
                }
            }

            // if already active than update the current location state.
            if (isActive()) {
                updateCurrentState();
            }
        }
        return config;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        logger.debug("Activate location [" + getLabel() + "]!");
        synchronized (serviceRemoteMapLock) {
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.activate();
                serviceRemote.addDataObserver(serviceDataObserver);
            }
            super.activate();
            presenceDetector.activate();
            updateCurrentState();
        }
    }

    private void updateCurrentState() {
        List<ServiceType> serviceTypes = new ArrayList<>();
        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
            for (final ServiceType serviceType : serviceTypes) {
                final Object state;

                try {
                    state = Service.invokeProviderServiceMethod(serviceType, this);
                } catch (NotAvailableException ex) {
                    logger.debug("No service data for type[" + serviceType + "] on location available!", ex);
                    continue;
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException("LocationController does not implement service[" + serviceType + "] method defined for the location unitType!", ex), logger);
                    continue;
                }

                try {
                    Service.invokeOperationServiceMethod(serviceType, dataBuilder.getInternalBuilder(), state);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException("Location RST data type does not contain data for service[" + serviceType + "]!", ex), logger);
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update current status!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            presenceDetector.deactivate();
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.removeDataObserver(serviceDataObserver);
                serviceRemote.deactivate();
            }
        }
        super.deactivate();
    }

    @Override
    public void registerMethods(RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Location.class, this, server);
    }

    private boolean isServiceTypeSupported(final ServiceType serviceType) throws CouldNotPerformException, InterruptedException {
        if (serviceType == null) {
            assert false;
            throw new NotAvailableException("ServiceType");
        }
        return getSupportedServiceTypes().contains(serviceType);
    }

    private Set<ServiceType> getSupportedServiceTypes() throws NotAvailableException, InterruptedException {
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        try {
            for (final ServiceConfig serviceConfig : getConfig().getServiceConfigList()) {
                serviceTypeSet.add(serviceConfig.getServiceTemplate().getType());
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SupportedServiceTypes", new CouldNotPerformException("Could not generate supported service type list!", ex));
        }
        return serviceTypeSet;
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }

    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        try {
            Snapshot.Builder snapshotBuilder = Snapshot.newBuilder();
            Set<UnitRemote> unitRemoteSet = new HashSet<>();
            synchronized (serviceRemoteMapLock) {
                serviceRemoteMap.values().stream().forEach((serviceRemote) -> {
                    unitRemoteSet.addAll(serviceRemote.getInternalUnits());
                });
            }
            for (UnitRemote remote : unitRemoteSet) {
                try {
                    if (UnitProcessor.isDalUnit(remote)) {
                        if (!remote.isConnected()) {
                            throw new NotAvailableException("Unit[" + remote.getLabel() + "] is currently not reachable!");
                        }
                        snapshotBuilder.addAllActionConfig(remote.recordSnapshot().get(2, TimeUnit.SECONDS).getActionConfigList());
                    }
                } catch (ExecutionException | TimeoutException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + remote.getLabel(), ex), logger);
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
            final Map<String, UnitRemote<?>> unitRemoteMap = new HashMap<>();
            synchronized (serviceRemoteMapLock) {
                for (AbstractServiceRemote<?, ?> serviceRemote : serviceRemoteMap.values()) {
                    for (UnitRemote<?> unitRemote : serviceRemote.getInternalUnits()) {
                        unitRemoteMap.put(unitRemote.getId(), unitRemote);
                    }
                }
            }

            for (final ActionConfig actionConfig : snapshot.getActionConfigList()) {
                unitRemoteMap.get(actionConfig.getUnitId()).applyAction(actionConfig);
            }
            return CompletableFuture.completedFuture(null);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    @Override
    public Future<Void> applyAction(final ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        Collection<Future> futureCollection = new ArrayList<>();
        synchronized (serviceRemoteMapLock) {
            for (Service service : serviceRemoteMap.values()) {
                futureCollection.add(service.applyAction(actionConfig));
            }
        }
        return GlobalCachedExecutorService.allOf(futureCollection, null);
    }

    @Override
    public List<String> getNeighborLocationIds() throws CouldNotPerformException {
        List<String> neighborIdList = new ArrayList<>();
        for (UnitConfig locationConfig : LocationManagerController.getInstance().getLocationRegistry().getNeighborLocations(getId())) {
            neighborIdList.add(locationConfig.getId());
        }
        return neighborIdList;
    }

    @Override
    public PresenceState getPresenceState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return getData().getPresenceState();
        }
    }

    @Override
    public Future<Void> setBlindState(BlindState blindState, UnitType unitType) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState, unitType);
        }
    }

    @Override
    public Future<Void> setBlindState(BlindState blindState) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState);
        }
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceType.BLIND_STATE_SERVICE)).getBlindState();
        }
    }

    @Override
    public BlindState getBlindState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceType.BLIND_STATE_SERVICE)).getBlindState(unitType);
        }
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState brightnessState) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState);
        }
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState brightnessState, UnitType unitType) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState, unitType);
        }
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState();
        }
    }

    @Override
    public BrightnessState getBrightnessState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState(unitType);
        }
    }

    @Override
    public Future<Void> setColorState(ColorState colorState) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState);
        }
    }

    @Override
    public Future<Void> setColorState(ColorState colorState, UnitType unitType) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState, unitType);
        }
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_STATE_SERVICE)).getColorState();
        }
    }

    @Override
    public ColorState getColorState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_STATE_SERVICE)).getColorState(unitType);
        }
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState);
        }
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState, UnitType unitType) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState, unitType);
        }
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_STATE_SERVICE)).getPowerState();
        }
    }

    @Override
    public PowerState getPowerState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_STATE_SERVICE)).getPowerState(unitType);
        }
    }

    @Override
    public Future<Void> setStandbyState(StandbyState standbyState) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(standbyState);
        }
    }

    @Override
    public Future<Void> setStandbyState(StandbyState state, UnitType unitType) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(state, unitType);
        }
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_STATE_SERVICE)).getStandbyState();
        }
    }

    @Override
    public StandbyState getStandbyState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_STATE_SERVICE)).getStandbyState(unitType);
        }
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState temperatureState) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState);
        }
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState temperatureState, UnitType unitType) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState, unitType);
        }
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState();
        }
    }

    @Override
    public TemperatureState getTargetTemperatureState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState(unitType);
        }
    }

    @Override
    public MotionState getMotionState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((MotionStateServiceRemote) serviceRemoteMap.get(ServiceType.MOTION_STATE_SERVICE)).getMotionState();
        }
    }

    @Override
    public MotionState getMotionState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((MotionStateServiceRemote) serviceRemoteMap.get(ServiceType.MOTION_STATE_SERVICE)).getMotionState(unitType);
        }
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((SmokeAlarmStateServiceRemote) serviceRemoteMap.get(ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState();
        }
    }

    @Override
    public AlarmState getSmokeAlarmState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((SmokeAlarmStateServiceRemote) serviceRemoteMap.get(ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState(unitType);
        }
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((SmokeStateServiceRemote) serviceRemoteMap.get(ServiceType.SMOKE_STATE_SERVICE)).getSmokeState();
        }
    }

    @Override
    public SmokeState getSmokeState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((SmokeStateServiceRemote) serviceRemoteMap.get(ServiceType.SMOKE_STATE_SERVICE)).getSmokeState(unitType);
        }
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((TemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState();
        }
    }

    @Override
    public TemperatureState getTemperatureState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((TemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState(unitType);
        }
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((PowerConsumptionStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState();
        }
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((PowerConsumptionStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState(unitType);
        }
    }

    @Override
    public TamperState getTamperState() throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
        }
        return ((TamperStateServiceRemote) serviceRemoteMap.get(ServiceType.TAMPER_STATE_SERVICE)).getTamperState();
    }

    @Override
    public TamperState getTamperState(UnitType unitType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            return ((TamperStateServiceRemote) serviceRemoteMap.get(ServiceType.TAMPER_STATE_SERVICE)).getTamperState(unitType);
        }
    }
}
