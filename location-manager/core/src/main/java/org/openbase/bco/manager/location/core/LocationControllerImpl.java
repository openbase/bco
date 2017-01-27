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
import org.openbase.bco.manager.location.lib.LocationController;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
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
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.PowerConsumptionStateType;
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

    private final List<String> originalUnitIdList;
    private final PresenceDetector presenceDetector;
    private UnitRegistry unitRegistry;
    private final Map<ServiceType, AbstractServiceRemote> serviceRemoteMap;
    private final ServiceRemoteFactory serviceRemoteFactory;
    private final Observer serviceDataObserver;

    public LocationControllerImpl() throws InstantiationException {
        super(LocationData.newBuilder());
        this.originalUnitIdList = new ArrayList<>();
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
                getCurrentStatus();
            }
        };
    }

    private boolean isSupportedServiceType(final ServiceType serviceType) {
        switch (serviceType) {
//            case BRIGHTNESS_STATE_SERVICE:
            case COLOR_STATE_SERVICE:
            case MOTION_STATE_SERVICE:
            case POWER_CONSUMPTION_STATE_SERVICE:
            case POWER_STATE_SERVICE:
            case BLIND_STATE_SERVICE:
            case SMOKE_ALARM_STATE_SERVICE:
            case SMOKE_STATE_SERVICE:
            case STANDBY_STATE_SERVICE:
            case TAMPER_STATE_SERVICE:
            case TARGET_TEMPERATURE_STATE_SERVICE:
            case TEMPERATURE_STATE_SERVICE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        logger.debug("Init location [" + config.getLabel() + "]");
        try {
            CachedUnitRegistryRemote.waitForData();
            unitRegistry = CachedUnitRegistryRemote.getRegistry();
            Map<ServiceType, List<UnitConfig>> serviceMap = new HashMap<>();
            for (ServiceType serviceType : ServiceType.values()) {
                if (isSupportedServiceType(serviceType)) {
                    serviceMap.put(serviceType, new ArrayList<>());
                }
            }
            for (UnitConfig unitConfig : unitRegistry.getDalUnitConfigs()) {
                Set<ServiceType> serviceTypeSet = new HashSet<>();
                if (config.getLocationConfig().getUnitIdList().contains(unitConfig.getId())) {
                    List<ServiceTemplate> serviceTemplates = unitRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTemplateList();
                    for (ServiceTemplate serviceTemplate : serviceTemplates) {
                        if (!serviceTypeSet.contains(serviceTemplate.getType()) && isSupportedServiceType(serviceTemplate.getType())) {
                            serviceTypeSet.add(serviceTemplate.getType());
                            serviceMap.get(serviceTemplate.getType()).add(unitConfig);
                        }
                    }
                }
            }

            for (ServiceType serviceType : serviceMap.keySet()) {
                serviceRemoteMap.put(serviceType, serviceRemoteFactory.newInitializedInstance(serviceType, serviceMap.get(serviceType)));
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        presenceDetector.init(this);
        super.init(config);
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        List<String> newUnitIdList = new ArrayList<>(config.getLocationConfig().getUnitIdList());
        for (String originalId : originalUnitIdList) {
            if (config.getLocationConfig().getUnitIdList().contains(originalId)) {
                newUnitIdList.remove(originalId);
            } else {
                UnitConfig unitConfig = unitRegistry.getUnitConfigById(originalId);
                for (ServiceTemplate serviceTemplate : unitRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTemplateList()) {
                    if (!isSupportedServiceType(serviceTemplate.getType())) {
                        continue;
                    }
                    serviceRemoteMap.get(serviceTemplate.getType()).removeUnit(unitConfig);
                }
            }
        }
        for (String newUnitId : newUnitIdList) {
            UnitConfig unitConfig = unitRegistry.getUnitConfigById(newUnitId);
            for (ServiceTemplate serviceTemplate : unitRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTemplateList()) {
                if (!isSupportedServiceType(serviceTemplate.getType())) {
                    continue;
                }

                serviceRemoteMap.get(serviceTemplate.getType()).init(unitConfig);
            }
        }
        if (isActive()) {
            getCurrentStatus();
        }
        originalUnitIdList.clear();
        originalUnitIdList.addAll(config.getLocationConfig().getUnitIdList());
        return super.applyConfigUpdate(config);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        logger.debug("Activate location [" + getLabel() + "]!");
        for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
            serviceRemote.activate();
            serviceRemote.addDataObserver(serviceDataObserver);
        }
        super.activate();
        presenceDetector.activate();
        getCurrentStatus();
    }

    private void getCurrentStatus() {
        List<ServiceType> serviceTypes = new ArrayList<>();
        for (ServiceType serviceType : serviceTypes) {
            try {
                Service.invokeServiceMethod(ServiceTemplate.newBuilder().setPattern(ServiceTemplate.ServicePattern.PROVIDER).setType(serviceType).build(), this);
            } catch (Exception ex) {

            }
        }
//        this.getClass().getMethod(FIELD_SCOPE, parameterTypes)
        try {
            BrightnessState brighntess = getBrightnessState();
            ColorState color = getColorState();
            MotionState motion = getMotionState();
            PowerState power = getPowerState();
            PowerConsumptionStateType.PowerConsumptionState powerConsumption = getPowerConsumptionState();
            BlindState blindState = getBlindState();
            AlarmState smokeAlarmState = getSmokeAlarmState();
            SmokeState smokeState = getSmokeState();
            StandbyState standby = getStandbyState();
            TamperState tamper = getTamperState();
            TemperatureState targetTemperature = getTargetTemperatureState();
            TemperatureState temperature = getTemperatureState();
            try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                dataBuilder.getInternalBuilder().setBrightnessState(brighntess);
                dataBuilder.getInternalBuilder().setColorState(color);
                dataBuilder.getInternalBuilder().setMotionState(motion);
                dataBuilder.getInternalBuilder().setPowerState(power);
                dataBuilder.getInternalBuilder().setPowerConsumptionState(powerConsumption);
                dataBuilder.getInternalBuilder().setBlindState(blindState);
                dataBuilder.getInternalBuilder().setSmokeAlarmState(smokeAlarmState);
                dataBuilder.getInternalBuilder().setSmokeState(smokeState);
                dataBuilder.getInternalBuilder().setStandbyState(standby);
                dataBuilder.getInternalBuilder().setTamperState(tamper);
                dataBuilder.getInternalBuilder().setTargetTemperatureState(targetTemperature);
                dataBuilder.getInternalBuilder().setAcutalTemperatureState(temperature);
                dataBuilder.getInternalBuilder().setPresenceState(presenceDetector.getData());
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not apply data change!", ex);
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not get current status!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        presenceDetector.deactivate();
        for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
            serviceRemote.removeDataObserver(serviceDataObserver);
            serviceRemote.deactivate();
        }
        super.deactivate();
    }

    @Override
    public void registerMethods(RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Location.class, this, server);
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
            serviceRemoteMap.values().stream().forEach((serviceRemote) -> {
                unitRemoteSet.addAll(serviceRemote.getInternalUnits());
            });
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
            for (AbstractServiceRemote<?, ?> serviceRemote : serviceRemoteMap.values()) {
                for (UnitRemote<?> unitRemote : serviceRemote.getInternalUnits()) {
                    unitRemoteMap.put(unitRemote.getId(), unitRemote);
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
        for (Service service : serviceRemoteMap.values()) {
            futureCollection.add(service.applyAction(actionConfig));
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
        return getData().getPresenceState();
    }

    @Override
    public Future<Void> setBlindState(BlindState blindState, UnitType unitType) throws CouldNotPerformException {
        return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState, unitType);
    }

    @Override
    public Future<Void> setBlindState(BlindState blindState) throws CouldNotPerformException {
        return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState);
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceType.BLIND_STATE_SERVICE)).getBlindState();
    }

    @Override
    public BlindState getBlindState(UnitType unitType) throws NotAvailableException {
        return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceType.BLIND_STATE_SERVICE)).getBlindState(unitType);
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState brightnessState) throws CouldNotPerformException {
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState);
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState brightnessState, UnitType unitType) throws CouldNotPerformException {
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState, unitType);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState();
    }

    @Override
    public BrightnessState getBrightnessState(UnitType unitType) throws NotAvailableException {
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState(unitType);
    }

    @Override
    public Future<Void> setColorState(ColorState colorState) throws CouldNotPerformException {
        return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState);
    }

    @Override
    public Future<Void> setColorState(ColorState colorState, UnitType unitType) throws CouldNotPerformException {
        return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState, unitType);
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_STATE_SERVICE)).getColorState();
    }

    @Override
    public ColorState getColorState(UnitType unitType) throws NotAvailableException {
        return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_STATE_SERVICE)).getColorState(unitType);
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState) throws CouldNotPerformException {
        return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState);
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState, UnitType unitType) throws CouldNotPerformException {
        return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState, unitType);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_STATE_SERVICE)).getPowerState();
    }

    @Override
    public PowerState getPowerState(UnitType unitType) throws NotAvailableException {
        return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_STATE_SERVICE)).getPowerState(unitType);
    }

    @Override
    public Future<Void> setStandbyState(StandbyState standbyState) throws CouldNotPerformException {
        return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(standbyState);
    }

    @Override
    public Future<Void> setStandbyState(StandbyState state, UnitType unitType) throws CouldNotPerformException {
        return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(state, unitType);
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_STATE_SERVICE)).getStandbyState();
    }

    @Override
    public StandbyState getStandbyState(UnitType unitType) throws NotAvailableException {
        return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_STATE_SERVICE)).getStandbyState(unitType);
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState temperatureState) throws CouldNotPerformException {
        return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState);
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState temperatureState, UnitType unitType) throws CouldNotPerformException {
        return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState, unitType);
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState();
    }

    @Override
    public TemperatureState getTargetTemperatureState(UnitType unitType) throws NotAvailableException {
        return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState(unitType);
    }

    @Override
    public MotionState getMotionState() throws NotAvailableException {
        return ((MotionStateServiceRemote) serviceRemoteMap.get(ServiceType.MOTION_STATE_SERVICE)).getMotionState();
    }

    @Override
    public MotionState getMotionState(UnitType unitType) throws NotAvailableException {
        return ((MotionStateServiceRemote) serviceRemoteMap.get(ServiceType.MOTION_STATE_SERVICE)).getMotionState(unitType);
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        return ((SmokeAlarmStateServiceRemote) serviceRemoteMap.get(ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState();
    }

    @Override
    public AlarmState getSmokeAlarmState(UnitType unitType) throws NotAvailableException {
        return ((SmokeAlarmStateServiceRemote) serviceRemoteMap.get(ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState(unitType);
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        return ((SmokeStateServiceRemote) serviceRemoteMap.get(ServiceType.SMOKE_STATE_SERVICE)).getSmokeState();
    }

    @Override
    public SmokeState getSmokeState(UnitType unitType) throws NotAvailableException {
        return ((SmokeStateServiceRemote) serviceRemoteMap.get(ServiceType.SMOKE_STATE_SERVICE)).getSmokeState(unitType);
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        return ((TemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState();
    }

    @Override
    public TemperatureState getTemperatureState(UnitType unitType) throws NotAvailableException {
        return ((TemperatureStateServiceRemote) serviceRemoteMap.get(ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState(unitType);
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        return ((PowerConsumptionStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState();
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState(UnitType unitType) throws NotAvailableException {
        return ((PowerConsumptionStateServiceRemote) serviceRemoteMap.get(ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState(unitType);
    }

    @Override
    public TamperState getTamperState() throws NotAvailableException {
        return ((TamperStateServiceRemote) serviceRemoteMap.get(ServiceType.TAMPER_STATE_SERVICE)).getTamperState();
    }

    @Override
    public TamperState getTamperState(UnitType unitType) throws NotAvailableException {
        return ((TamperStateServiceRemote) serviceRemoteMap.get(ServiceType.TAMPER_STATE_SERVICE)).getTamperState(unitType);
    }
}
