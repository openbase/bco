package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class NearFieldLightAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;
    private List<LocationRemote> neighborRemotes;

    public NearFieldLightAgent() throws InstantiationException {
        super();
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        try {
            super.init(config);
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            neighborRemotes = locationRemote.getNeighborLocationList(false);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("LocationRemote not available", ex);
        }

//        try {
//            for (LocationRemote neighborRemote : neighborRemotes) {
//                if (locationRemote.hasDirectConnection(neighborRemote.getId(), ConnectionType.PASSAGE, true)) {
//                    GenericBCOTrigger<LocationRemote, LocationData, PresenceState.State> trigger = new GenericBCOTrigger<>(neighborRemote, PresenceState.State.PRESENT, ServiceType.PRESENCE_STATE_SERVICE);
//                    agentTriggerHolder.addTrigger(trigger, TriggerAggregation.OR);
//                } else {
//                    for (ConnectionRemote relatedConnection : locationRemote.getDirectConnectionList(neighborRemote.getId(), false)) {
//                        NeighborConnectionPresenceTrigger trigger = new NeighborConnectionPresenceTrigger(neighborRemote, relatedConnection);
//                        agentTriggerHolder.addTrigger(trigger, TriggerAggregation.OR);
//                    }
//                }
//            }
//        } catch (CouldNotPerformException ex) {
//            throw new InitializationException("Could not initialize trigger", ex);
//        }
    }

    @Override
    protected void trigger(ActivationState activationState) throws CouldNotPerformException, InterruptedException {
//        try {
//            ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
//                    ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
//                    1000 * 30,
//                    ResourceAllocationType.ResourceAllocation.Policy.FIRST,
//                    ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
//                    locationRemote,
//                    BrightnessState.newBuilder().setBrightness(0.5).build(),
//                    UnitTemplateType.UnitTemplate.UnitType.DIMMABLE_LIGHT,
//                    ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE,
//                    MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
//            actionRescheduleHelper.startActionRescheduleing(locationRemote.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
//        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
//            logger.error("Could not dim lights.", ex);
//        }
    }
}
