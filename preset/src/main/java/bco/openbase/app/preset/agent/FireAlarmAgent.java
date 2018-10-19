package bco.openbase.app.preset.agent;

/*-
 * #%L
 * BCO Manager Agent Core
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
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.unit.UnitConfigType;

import java.util.concurrent.ExecutionException;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class FireAlarmAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;
    private final AlarmState.State triggerState = AlarmState.State.ALARM;

    public FireAlarmAgent() throws InstantiationException {
        super(FireAlarmAgent.class);

//        actionRescheduleHelper = new ActionRescheduler(ActionRescheduler.RescheduleOption.EXTEND, 30);

//        triggerHolderObserver = (Trigger source, ActivationState data) -> {
//            if (data.getValue().equals(ActivationState.State.ACTIVE)) {
//                alarmRoutine();
//            } else {
////                actionRescheduleHelper.stopExecution();
//            }
//        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);

        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
        } catch (NotAvailableException ex) {
            throw new InitializationException("LocationRemote not available.", ex);
        }

//        try {
//            GenericBCOTrigger<LocationRemote, LocationData, AlarmState.State> agentTrigger = new GenericBCOTrigger(locationRemote, triggerState, ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_ALARM_STATE_SERVICE);
//            agentTriggerHolder.addTrigger(agentTrigger, TriggerAggregation.OR);
//            GenericBCOTrigger<LocationRemote, LocationData, AlarmState.State> agentFireTrigger = new GenericBCOTrigger(locationRemote, triggerState, ServiceTemplateType.ServiceTemplate.ServiceType.FIRE_ALARM_STATE_SERVICE);
//            agentTriggerHolder.addTrigger(agentFireTrigger, TriggerAggregation.OR);
//        } catch (CouldNotPerformException ex) {
//            throw new InitializationException("Could not add agent to agentpool", ex);
//        }
    }

    private void alarmRoutine() {
//        try {
//            ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
//                    ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
//                    1000 * 30,
//                    ResourceAllocationType.ResourceAllocation.Policy.FIRST,
//                    ResourceAllocationType.ResourceAllocation.Priority.EMERGENCY,
//                    locationRemote,
//                    PowerState.newBuilder().setValue(PowerState.State.ON).build(),
//                    UnitType.LIGHT,
//                    ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE,
//                    MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
//            actionRescheduleHelper.startActionRescheduleing(locationRemote.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
//
//            actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
//                    ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
//                    1000 * 30,
//                    ResourceAllocationType.ResourceAllocation.Policy.FIRST,
//                    ResourceAllocationType.ResourceAllocation.Priority.EMERGENCY,
//                    locationRemote,
//                    BlindStateType.BlindState.newBuilder().setOpeningRatio(100.0).build(),
//                    UnitType.UNKNOWN,
//                    ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE,
//                    MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
//            actionRescheduleHelper.addRescheduleAction(locationRemote.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
//
//            // TODO: Maybe also set Color and Brightness?
//        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
//            logger.error("Could not execute alarm routine.", ex);
//        }
    }

    @Override
    void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {

    }
}
