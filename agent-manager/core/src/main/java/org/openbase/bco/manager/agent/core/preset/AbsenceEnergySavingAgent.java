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

import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.trigger.GenericBCOTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AbsenceEnergySavingAgent extends AbstractTriggerableAgent {

    private LocationRemote locationRemote;

    public AbsenceEnergySavingAgent() throws InstantiationException {
        super(AbsenceEnergySavingAgent.class);
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);
            registerTrigger(new GenericBCOTrigger(locationRemote, PresenceState.State.ABSENT, ServiceType.PRESENCE_STATE_SERVICE), TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    ActionDescription actionDescription;

    @Override
    void trigger(ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                actionDescription = locationRemote.applyAction(generateAction(UnitType.UNKNOWN, ServiceType.POWER_STATE_SERVICE, PowerState.newBuilder().setValue(State.OFF))).get();
                break;
            case DEACTIVE:
                if(actionDescription != null) {
                    actionDescription = locationRemote.cancelAction(actionDescription).get();
                }
                break;
        }
    }

    private void switchlightsOff() {

    }

//    private void switchMultimediaOff() {
//        try {
//            locationRemote.applyAction(generateAction(UnitType.LIGHT, ServiceType.POWER_STATE_SERVICE, PowerState.newBuilder().setValue(PowerState.State.OFF))).get();
//        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
//            logger.error("Could not switch off Lights.", ex);
//        }
//
//        try {
//            List<? extends UnitGroupRemote> unitsByLabel = Units.getUnitsByLabel(locationRemote.getLabel().concat("MultimediaGroup"), true, Units.UNITGROUP);
//            if (!unitsByLabel.isEmpty()) {
//                UnitGroupRemote multimediaGroup = unitsByLabel.get(0);
//                ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
//                        ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
//                        1000 * 30,
//                        ResourceAllocationType.ResourceAllocation.Policy.FIRST,
//                        ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
//                        multimediaGroup,
//                        PowerState.newBuilder().setValue(PowerState.State.OFF).build(),
//                        UnitType.UNKNOWN,
//                        ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE,
//                        MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
//                actionRescheduleHelper.addRescheduleAction(multimediaGroup.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
//            }
//        } catch (NotAvailableException ex) {
//            logger.info("MultimediaGroup not available.");
//        } catch (InterruptedException ex) {
//            logger.error("Could not get MultimediaGroup!");
//        } catch (CouldNotPerformException ex) {
//            logger.error("Could not set Powerstate of MultimediaGroup.");
//        } catch (ExecutionException ex) {
//            logger.error("Could not set Powerstate of MultimediaGroup!");
//        }
//    }
}
