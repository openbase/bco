package bco.openbase.app.preset.agent;

/*
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
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType;

import java.util.concurrent.ExecutionException;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class RandomLightPatternAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;
    private Thread thread;
    private final PresenceState.State triggerState = PresenceState.State.ABSENT;

    public RandomLightPatternAgent() throws InstantiationException {
        super(RandomLightPatternAgent.class);

//        actionRescheduleHelper = new ActionRescheduler(ActionRescheduler.RescheduleOption.EXTEND, 30);

//        triggerHolderObserver = (Trigger source, ActivationStateType.ActivationState data) -> {
//            GlobalCachedExecutorService.submit(() -> {
//                if (data.getValue().equals(ActivationStateType.ActivationState.State.ACTIVE)) {
//                    makeRandomLightPattern();
//                } else {
//                    stopRandomLightPattern();
//                }
//                return null;
//            });
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
//
//        try {
//            GenericBCOTrigger<LocationRemote, LocationDataType.LocationData, PresenceStateType.PresenceState.State> agentTrigger = new GenericBCOTrigger(locationRemote, triggerState, ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE);
//            agentTriggerHolder.addTrigger(agentTrigger, TriggerAggregation.OR);
//        } catch (CouldNotPerformException ex) {
//            throw new InitializationException("Could not add agent to agentpool", ex);
//        }
    }

    @Override
    protected void stop(final ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        stopRandomLightPattern();
        super.stop(activationState);
    }

    @Override
    void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {

    }

    private void makeRandomLightPattern() {
        thread = new PersonSimulator();
        thread.start();
    }

    private void stopRandomLightPattern() {
        thread.interrupt();
    }

    private class PersonSimulator extends Thread {

        @Override
        public void run() {
//            try {
//                List<LocationRemote> childLocationList = locationRemote.getChildLocationList(true);
//                LocationRemote currentLocation = childLocationList.get(ThreadLocalRandom.current().nextInt(childLocationList.size()));
//
//                while (true) {
//                    ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
//                            ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
//                            1000 * 30,
//                            ResourceAllocationType.ResourceAllocation.Policy.FIRST,
//                            ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
//                            currentLocation,
//                            PowerState.newBuilder().setValue(PowerState.State.ON).build(),
//                            UnitType.LIGHT,
//                            ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE,
//                            MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
//                    actionRescheduleHelper.startActionRescheduleing(currentLocation.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
//
//                    //TODO: waiting time
//                    Thread.sleep(600000);
//                    actionRescheduleHelper.stopExecution();
//                    actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
//                            ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
//                            1000 * 30,
//                            ResourceAllocationType.ResourceAllocation.Policy.FIRST,
//                            ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
//                            currentLocation,
//                            PowerState.newBuilder().setValue(PowerState.State.OFF).build(),
//                            UnitType.LIGHT,
//                            ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE,
//                            MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
//                    currentLocation.applyAction(actionDescriptionBuilder.build()).get().toBuilder();
//
//                    List<LocationRemote> neighborLocationList = currentLocation.getNeighborLocationList(true);
//                    currentLocation = neighborLocationList.get(ThreadLocalRandom.current().nextInt(neighborLocationList.size()));
//                }
//            } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
//                Logger.getLogger(RandomLightPatternAgent.class.getName()).log(Level.SEVERE, null, ex);
//                interrupt();
//            }
        }

        @Override
        public void interrupt() {
//            actionRescheduleHelper.stopExecution();

            super.interrupt();
        }
    }
}
