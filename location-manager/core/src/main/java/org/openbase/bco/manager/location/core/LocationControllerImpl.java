package org.openbase.bco.manager.location.core;

/*
 * #%L
 * BCO Manager Location Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.dal.remote.detector.PresenceDetector;
import org.openbase.bco.dal.remote.processing.StandbyController;
import org.openbase.bco.dal.remote.service.ServiceRemoteManager;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.location.lib.LocationController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.*;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.state.StandbyStateType.StandbyState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.domotic.unit.location.LocationDataType.LocationData.Builder;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static org.openbase.bco.dal.remote.unit.Units.LOCATION;
import static org.openbase.bco.manager.location.core.LocationManagerController.LOGGER;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE;

/**
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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescriptionType.ActionDescription.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Snapshot.getDefaultInstance()));
    }

    private final PresenceDetector presenceDetector;
    private final ServiceRemoteManager serviceRemoteManager;
    private final RecurrenceEventFilter unitEventFilter;

    public LocationControllerImpl() throws InstantiationException {
        super(LocationControllerImpl.class, LocationData.newBuilder());

        try {
            // update location data on updates from internal units at most every 100ms
            unitEventFilter = new RecurrenceEventFilter(10) {
                @Override
                public void relay() throws Exception {
                    updateUnitData();
                }
            };
            this.serviceRemoteManager = new ServiceRemoteManager(this) {
                @Override
                protected Set<ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException {
                    return LocationControllerImpl.this.getSupportedServiceTypes();
                }

                @Override
                protected void notifyServiceUpdate(Observable source, Object data) throws NotAvailableException {
                    try {
                        unitEventFilter.trigger();
                    } catch (final CouldNotPerformException ex) {
                        logger.error("Could not trigger recurrence event filter for location[" + getLabel() + "]");
                    }
                }
            };

            registerOperationService(ServiceType.STANDBY_STATE_SERVICE, new StandbyStateOperationServiceImpl(this));

            this.presenceDetector = new PresenceDetector();
            this.presenceDetector.addDataObserver(new Observer<PresenceState>() {
                @Override
                public void update(Observable<PresenceState> source, PresenceState data) throws Exception {
                    try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                        LOGGER.debug("Set " + this + " presence to [" + data.getValue() + "]");
                        dataBuilder.getInternalBuilder().setPresenceState(data);
                    } catch (CouldNotPerformException ex) {
                        throw new CouldNotPerformException("Could not apply presense state change!", ex);
                    }
                }
            });


        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final UnitConfig unitConfig) throws InitializationException, InterruptedException {
        try {
            Registries.waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(unitConfig);

        // do not notify because not activated yet
        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this, false)) {
            dataBuilder.getInternalBuilder().setStandbyState(StandbyState.newBuilder().setValue(StandbyState.State.RUNNING).build());
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not apply initial standby service states!", ex), LOGGER, LogLevel.WARN);
        }

        presenceDetector.init(this);

    }

    @Override
    public synchronized UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        serviceRemoteManager.applyConfigUpdate(unitConfig.getLocationConfig().getUnitIdList());
        // if already active than update the current location state.
        if (isActive()) {
            updateUnitData();
        }
        return unitConfig;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (isActive()) {
            LOGGER.debug("Skipp location controller activations because is already active...");
            return;
        }
        LOGGER.debug("Activate location [" + getLabel() + "]!");
        super.activate();
        serviceRemoteManager.activate();
        presenceDetector.activate();
        updateUnitData();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        LOGGER.debug("Deactivate location [" + getLabel() + "]!");
        super.deactivate();
        serviceRemoteManager.deactivate();
        presenceDetector.deactivate();
    }

    private void updateUnitData() throws InterruptedException {
        try (ClosableDataBuilder<LocationDataType.LocationData.Builder> dataBuilder = getDataBuilder(this)) {
            serviceRemoteManager.updateBuilderWithAvailableServiceStates(dataBuilder.getInternalBuilder(), getDataClass(), getSupportedServiceTypes());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update current status!", ex), LOGGER, LogLevel.WARN);
        }
    }

    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.recordSnapshot();
    }

    @Override
    public Future<Snapshot> recordSnapshot(final UnitType unitType) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.recordSnapshot(unitType);
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.restoreSnapshot(snapshot);
    }

    @Override
    protected Future<Void> internalRestoreSnapshot(Snapshot snapshot, AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.restoreSnapshotAuthenticated(snapshot, authenticationBaseData);
    }

    @Override
    public Future<ActionFuture> applyAction(final ActionDescription actionDescription) throws CouldNotPerformException {
        switch (actionDescription.getServiceStateDescription().getServiceType()) {
            case STANDBY_STATE_SERVICE:
                return super.applyAction(actionDescription);
            default:
                return serviceRemoteManager.applyAction(actionDescription);
        }
    }

    @Override
    public ServiceRemote getServiceRemote(final ServiceType serviceType) throws NotAvailableException {
        return serviceRemoteManager.getServiceRemote(serviceType);
    }

    @Override
    public Future<ActionFuture> setStandbyState(final StandbyState standbyState) throws CouldNotPerformException {
        return applyUnauthorizedAction(standbyState, STANDBY_STATE_SERVICE);
    }

    public List<LocationRemote> getChildLocationList(final boolean waitForData) throws CouldNotPerformException {
        final List<LocationRemote> childList = new ArrayList<>();
        for (String childId : getConfig().getLocationConfig().getChildIdList()) {
            try {
                childList.add(Units.getUnit(Registries.getUnitRegistry().getUnitConfigById(childId), waitForData, LOCATION));
            } catch (InterruptedException ex) {
                throw new CouldNotPerformException("Could not get all child locations!", ex);
            }
        }
        return childList;
    }

    public class StandbyStateOperationServiceImpl implements StandbyStateOperationService {

        private LocationController locationController;
        private StandbyController<LocationController> standbyController;

        public StandbyStateOperationServiceImpl(final LocationController locationController) {
            this.locationController = locationController;
            this.standbyController = new StandbyController();
            standbyController.init(locationController);
        }

        @Override
        public Future<ActionFuture> setStandbyState(StandbyState standbyState) throws CouldNotPerformException {
            LOGGER.info("Standby[" + standbyState.getValue() + "]" + this);
            return GlobalScheduledExecutorService.submit(() -> {
                try (ClosableDataBuilder<Builder> dataBuilder = locationController.getDataBuilder(this)) {
                    switch (getStandbyState().getValue()) {
                        case UNKNOWN:
                        case RUNNING:
                            switch (standbyState.getValue()) {
                                case STANDBY:
                                    for (LocationRemote childLocation : getChildLocationList(false)) {
                                        childLocation.waitForMiddleware();
                                        childLocation.setStandbyState(State.STANDBY).get();
                                    }
                                    standbyController.standby();
                                    dataBuilder.getInternalBuilder().setStandbyState(standbyState);
                            }
                            break;
                        case STANDBY:
                            switch (standbyState.getValue()) {
                                case RUNNING:
                                    standbyController.wakeup();
                                    for (LocationRemote childLocation : getChildLocationList(false)) {
                                        childLocation.waitForMiddleware();
                                        childLocation.setStandbyState(State.RUNNING).get();
                                    }
                                    dataBuilder.getInternalBuilder().setStandbyState(standbyState);
                                case STANDBY:
                                    // already in standby but the command is send again
                                    // make sure that all children are in standby
                                    for (LocationRemote childLocation : getChildLocationList(false)) {
                                        childLocation.waitForMiddleware();
                                        childLocation.setStandbyState(State.STANDBY).get();
                                    }
                            }
                    }
                    return null;
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not apply data change!", ex);
                }
            });
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return null;
        }
    }
}
