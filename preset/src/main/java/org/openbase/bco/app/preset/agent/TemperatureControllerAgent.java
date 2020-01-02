package org.openbase.bco.app.preset.agent;

/*-
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.openbase.bco.dal.remote.layer.unit.TemperatureControllerRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.unit.UnitConfigType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class TemperatureControllerAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;
    private final Map<TemperatureControllerRemote, TemperatureState> previousTemperatureState;
    private final PresenceState.State triggerState = PresenceState.State.ABSENT;

    public TemperatureControllerAgent() throws InstantiationException {

        previousTemperatureState = new HashMap();

//        actionRescheduleHelper = new ActionRescheduler(ActionRescheduler.RescheduleOption.EXTEND, 30);

//        triggerHolderObserver = (Trigger source, ActivationState data) -> {
//            if (data.getValue().equals(ActivationState.State.ACTIVE)) {
//                regulateTemperature();
//            } else {
////                actionRescheduleHelper.stopExecution();
//                restoreTemperature();
//            }
//        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        try {
            super.init(config);

            try {
                locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            } catch (NotAvailableException ex) {
                throw new InitializationException("LocationRemote not available.", ex);
            }

//            try {
//                GenericBCOTrigger<LocationRemote, LocationDataType.LocationData, PresenceState.State> agentTrigger = new GenericBCOTrigger(locationRemote, triggerState, ServiceType.PRESENCE_STATE_SERVICE);
//                agentTriggerHolder.addTrigger(agentTrigger, TriggerAggregation.OR);
//            } catch (CouldNotPerformException ex) {
//                throw new InitializationException("Could not add agent to agentpool", ex);
//            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not initialize Agent.", ex);
        }
    }

    private void regulateTemperature() {
//        previousTemperatureState.clear();
//        try {
//            for (TemperatureControllerRemote remote : locationRemote.getUnits(UnitType.TEMPERATURE_CONTROLLER, true, Units.TEMPERATURE_CONTROLLER)) {
//                previousTemperatureState.put(remote, remote.getTargetTemperatureState());
//
//            }
//        } catch (CouldNotPerformException | InterruptedException ex) {
//            logger.error("Could not get all TemperatureControllerRemotes.", ex);
//        }
//
//        try {
//            ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
//                    ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
//                    1000 * 30,
//                    ResourceAllocationType.ResourceAllocation.Policy.FIRST,
//                    ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
//                    locationRemote,
//                    TimestampProcessor.updateTimestampWithCurrentTime(TemperatureState.newBuilder().setTemperature(13.0).build()),
//                    UnitType.TEMPERATURE_CONTROLLER,
//                    ServiceTemplateType.ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE,
//                    MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
//            actionRescheduleHelper.startActionRescheduleing(locationRemote.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
//        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
//            logger.error("Could not set targetTemperatureState.", ex);
//        }
    }

    private void restoreTemperature() {
//        if (previousTemperatureState == null | previousTemperatureState.isEmpty()) {
//            return;
//        }
//
//        previousTemperatureState.forEach((remote, temperatureState) -> {
//            try {
//                ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
//                        ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
//                        1000 * 30,
//                        ResourceAllocationType.ResourceAllocation.Policy.FIRST,
//                        ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
//                        remote,
//                        TimestampProcessor.updateTimestampWithCurrentTime(temperatureState),
//                        UnitType.TEMPERATURE_CONTROLLER,
//                        ServiceTemplateType.ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE,
//                        MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
//                remote.applyAction(actionDescriptionBuilder.build()).get().toBuilder();
//            } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
//                logger.error("Could not restore targetTemperatureState.", ex);
//            }
//        });
//        previousTemperatureState.clear();
    }

    @Override
    protected void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {

    }
}
