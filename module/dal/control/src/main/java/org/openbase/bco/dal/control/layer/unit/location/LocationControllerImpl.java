package org.openbase.bco.dal.control.layer.unit.location;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.control.layer.unit.AbstractAggregatedBaseUnitController;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.location.LocationController;
import org.openbase.bco.dal.remote.detector.PresenceDetector;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.processing.StandbyController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.openbase.bco.dal.remote.layer.unit.Units.LOCATION;

/**
 * UnitConfig
 */
public class LocationControllerImpl extends AbstractAggregatedBaseUnitController<LocationData, Builder> implements LocationController {

    private final PresenceDetector presenceDetector;

    public LocationControllerImpl() throws InstantiationException {
        super(LocationData.newBuilder());

        try {
            registerOperationService(ServiceType.STANDBY_STATE_SERVICE, new StandbyStateOperationServiceImpl(this));
            registerOperationService(ServiceType.EMPHASIS_STATE_SERVICE, SIMPLE_STATE_ADOPTER);

            this.presenceDetector = new PresenceDetector();
            this.presenceDetector.addDataObserver(new Observer<DataProvider<PresenceState>, PresenceState>() {
                @Override
                public void update(DataProvider<PresenceState> source, PresenceState data) throws Exception {
                    try {
                        LocationManagerImpl.LOGGER.debug("Set " + this + " presence to [" + data.getValue() + "]");
                        applyServiceState(data, ServiceType.PRESENCE_STATE_SERVICE);
                    } catch (CouldNotPerformException ex) {
                        throw new CouldNotPerformException("Could not apply presence state change!", ex);
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

        try {
            applyServiceState(StandbyState.newBuilder().setValue(State.RUNNING), ServiceType.STANDBY_STATE_SERVICE);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not apply initial standby service state!", ex), LocationManagerImpl.LOGGER, LogLevel.WARN);
        }
        presenceDetector.init(this);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (isActive()) {
            return;
        }
        presenceDetector.activate();
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        presenceDetector.deactivate();
    }

    public List<LocationRemote> getChildLocationList(final boolean waitForData) throws CouldNotPerformException {
        final List<LocationRemote> childList = new ArrayList<>();
        for (String childId : getConfig().getLocationConfig().getChildIdList()) {
            try {
                childList.add(Units.getUnit(Registries.getUnitRegistry().getUnitConfigById(childId), waitForData, LOCATION));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CouldNotPerformException("Could not get all child locations!", ex);
            }
        }
        return childList;
    }

    @Override
    protected void applyCustomDataUpdate(Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case EMPHASIS_STATE_SERVICE:
                try {
                    for (LocationRemote childLocation : getChildLocationList(false)) {
                        childLocation.setEmphasisState(internalBuilder.getEmphasisState(), ActionParameter.newBuilder().setCause(internalBuilder.getEmphasisState().getResponsibleAction()).build());
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update childs emphasis statee.", ex, logger);
                }
                break;
        }
    }

    @Deprecated
    public class StandbyStateOperationServiceImpl implements StandbyStateOperationService {

        private StandbyController<LocationController> standbyController;

        private final ServiceProvider serviceProvider;

        public StandbyStateOperationServiceImpl(final ServiceProvider serviceProvider) {
            this.serviceProvider = serviceProvider;
            this.standbyController = new StandbyController();
            standbyController.init(LocationControllerImpl.this);
        }

        @Override
        public Future<ActionDescription> setStandbyState(StandbyState standbyState) {
            logger.info("Standby[" + standbyState + "]" + this);
            return GlobalScheduledExecutorService.submit(() -> {
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
                            case STANDBY:
                                // already in standby but the command is send again
                                // make sure that all children are in standby
                                for (LocationRemote childLocation : getChildLocationList(false)) {
                                    childLocation.waitForMiddleware();
                                    childLocation.setStandbyState(State.STANDBY).get();
                                }
                        }
                }

                try {
                    applyServiceState(standbyState, ServiceType.STANDBY_STATE_SERVICE);
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not update standby state of " + this + " to " + standbyState.getValue().name(), ex);
                }

                return null;
            });
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return serviceProvider;
        }
    }
}
