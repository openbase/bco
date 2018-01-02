package org.openbase.bco.manager.agent.core.preset;

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
import java.util.concurrent.ExecutionException;
import org.openbase.bco.dal.remote.trigger.GenericBCOTrigger;
import org.openbase.bco.dal.remote.action.ActionRescheduler;
import org.openbase.bco.dal.remote.action.ActionRescheduler.RescheduleOption;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.jul.pattern.trigger.TriggerPool;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class PresenceLightAgent extends AbstractResourceAllocationAgent {

    private LocationRemote locationRemote;
    private final PresenceState.State triggerState = PresenceState.State.PRESENT;

    public PresenceLightAgent() throws InstantiationException {
        super(PresenceLightAgent.class);

        actionRescheduleHelper = new ActionRescheduler(RescheduleOption.EXTEND, 30);

        triggerHolderObserver = (Observable<ActivationState> source, ActivationState data) -> {
            logger.warn("New trigger state: " + data.getValue());
            GlobalCachedExecutorService.submit(() -> {
                if (data.getValue().equals(ActivationState.State.ACTIVE)) {
                    switchlightsOn();
                } else {
                    logger.warn("Stop execution");
                    actionRescheduleHelper.stopExecution();
                }
                return null;
            });
        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);
        } catch (NotAvailableException ex) {
            throw new InitializationException("LocationRemote not available.", ex);
        }

        try {
            GenericBCOTrigger<LocationRemote, LocationData, PresenceState.State> agentTrigger = new GenericBCOTrigger(locationRemote, triggerState, ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE);
            agentTriggerHolder.addTrigger(agentTrigger, TriggerPool.TriggerOperation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not add agent to agentpool", ex);
        }
    }

    private void switchlightsOn() {
        try {
            ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthority.getDefaultInstance(),
                    ResourceAllocation.Initiator.SYSTEM,
                    1000 * 30,
                    ResourceAllocation.Policy.FIRST,
                    ResourceAllocation.Priority.NORMAL,
                    locationRemote,
                    PowerState.newBuilder().setValue(PowerState.State.ON).build(),
                    UnitType.LIGHT,
                    ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE,
                    MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
            actionRescheduleHelper.startActionRescheduleing(locationRemote.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            logger.error("Could not switch on Lights.", ex);
        }
    }
}
