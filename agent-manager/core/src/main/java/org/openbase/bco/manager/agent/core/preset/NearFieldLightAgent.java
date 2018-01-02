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
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.openbase.bco.dal.remote.trigger.GenericBCOTrigger;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.action.ActionRescheduler;
import org.openbase.bco.dal.remote.trigger.preset.NeighborConnectionPresenceTrigger;
import org.openbase.jul.pattern.trigger.TriggerPool;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Observable;
import rst.communicationpatterns.ResourceAllocationType;
import rst.domotic.action.ActionAuthorityType;
import rst.domotic.action.ActionDescriptionType;
import rst.domotic.action.MultiResourceAllocationStrategyType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class NearFieldLightAgent extends AbstractResourceAllocationAgent {

    private LocationRemote locationRemote;
    private List<LocationRemote> neighborRemotes;

    public NearFieldLightAgent() throws InstantiationException {
        super(NearFieldLightAgent.class);

        actionRescheduleHelper = new ActionRescheduler(ActionRescheduler.RescheduleOption.EXTEND, 30);

        triggerHolderObserver = (Observable<ActivationState> source, ActivationState data) -> {
            if (data.getValue().equals(ActivationState.State.ACTIVE)) {
                dimmLights();
            } else {
                actionRescheduleHelper.stopExecution();
            }
        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        try {
            super.init(config);
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);
            neighborRemotes = locationRemote.getNeighborLocationList(true);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("LocationRemote not available", ex);
        }

        try {
            for (LocationRemote neigborRemote : neighborRemotes) {
                if (locationRemote.hasDirectConnection(neigborRemote.getId(), ConnectionType.PASSAGE, true)) {
                    GenericBCOTrigger<LocationRemote, LocationData, PresenceState.State> trigger = new GenericBCOTrigger<>(neigborRemote, PresenceState.State.PRESENT, ServiceType.PRESENCE_STATE_SERVICE);
                    agentTriggerHolder.addTrigger(trigger, TriggerPool.TriggerOperation.OR);
                } else {
                    for (ConnectionRemote relatedConnection : locationRemote.getDirectConnectionList(neigborRemote.getId(), true)) {
                        NeighborConnectionPresenceTrigger trigger = new NeighborConnectionPresenceTrigger(neigborRemote, relatedConnection);
                        agentTriggerHolder.addTrigger(trigger, TriggerPool.TriggerOperation.OR);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not initialize trigger", ex);
        }
    }

    private void dimmLights() {
        try {
            ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
                    ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
                    1000 * 30,
                    ResourceAllocationType.ResourceAllocation.Policy.FIRST,
                    ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
                    locationRemote,
                    BrightnessState.newBuilder().setBrightness(0.5).build(),
                    UnitTemplateType.UnitTemplate.UnitType.DIMMABLE_LIGHT,
                    ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE,
                    MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
            actionRescheduleHelper.startActionRescheduleing(locationRemote.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            logger.error("Could not dim lights.", ex);
        }
    }
}
