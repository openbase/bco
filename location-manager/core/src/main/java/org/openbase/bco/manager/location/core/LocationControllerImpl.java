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
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.MotionStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.PowerConsumptionStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.TamperStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.TemperatureStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitProcessor;
import org.openbase.bco.dal.lib.layer.unit.location.Location;
import org.openbase.bco.dal.remote.detector.PresenceDetector;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.BlindStateServiceRemote;
import org.openbase.bco.dal.remote.service.BrightnessStateServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactory;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactoryImpl;
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
                    throw new CouldNotPerformException("Could not apply data change!", ex);
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
            case BRIGHTNESS_STATE_SERVICE:
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
            serviceRemote.addDataObserver(serviceDataObserver);
        }
        super.activate();
        presenceDetector.activate();
        getCurrentStatus();
    }

    private void getCurrentStatus() {
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
    public Future<Void> setBrightnessState(BrightnessState brightnessState) throws CouldNotPerformException {
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState);
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
    public BrightnessState getBrightnessState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<Void> setColorState(ColorState colorState) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<Void> setPowerState(PowerState powerState) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<Void> setStandbyState(StandbyState standbyState) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState temperatureState) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MotionState getMotionState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TamperState getTamperState() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<BrightnessStateOperationService> getBrightnessStateOperationServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<ColorStateOperationService> getColorStateOperationServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<PowerStateOperationService> getPowerStateOperationServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<BlindStateOperationService> getBlindStateOperationServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<StandbyStateOperationService> getStandbyStateOperationServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<TargetTemperatureStateOperationService> getTargetTemperatureStateOperationServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<MotionStateProviderService> getMotionStateProviderServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<SmokeAlarmStateProviderService> getSmokeAlarmStateProviderServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<SmokeStateProviderService> getSmokeStateProviderServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<TemperatureStateProviderService> getTemperatureStateProviderServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<PowerConsumptionStateProviderService> getPowerConsumptionStateProviderServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<TamperStateProviderService> getTamperStateProviderServices() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
