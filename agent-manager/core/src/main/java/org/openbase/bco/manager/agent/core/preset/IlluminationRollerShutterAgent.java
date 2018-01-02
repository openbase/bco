package org.openbase.bco.manager.agent.core.preset;

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
import java.util.concurrent.ExecutionException;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.action.ActionRescheduler;
import org.openbase.bco.dal.remote.trigger.preset.IlluminanceDualBoundaryTrigger;
import org.openbase.jul.pattern.trigger.TriggerPool;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.Observable;
import rst.communicationpatterns.ResourceAllocationType;
import rst.domotic.action.ActionAuthorityType;
import rst.domotic.action.ActionDescriptionType;
import rst.domotic.action.MultiResourceAllocationStrategyType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.BlindStateType;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.location.LocationDataType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class IlluminationRollerShutterAgent extends AbstractResourceAllocationAgent {

    public static final String MINIMUM_NEEDED_KEY = "MINIMUM_ILLUMINATION";
    public static final String MAXIMUM_WANTED_KEY = "MAXIMUM_ILLUMINATION";
    private static double MINIMUM_NEEDED_ILLUMINATION = 20000;
    private static double MAXIMUM_WANTED_ILLUMINATION = 40000;

    private LocationRemote locationRemote;

    public IlluminationRollerShutterAgent() throws InstantiationException {
        super(IlluminationRollerShutterAgent.class);

        actionRescheduleHelper = new ActionRescheduler(ActionRescheduler.RescheduleOption.EXTEND, 30);

        triggerHolderObserver = (Observable<ActivationState> source, ActivationState data) -> {
            if (data.getValue().equals(ActivationState.State.ACTIVE)) {
                if (locationRemote.getIlluminanceState().getIlluminance() > MAXIMUM_WANTED_ILLUMINATION) {
                    regulateShutterLevelDown();

                } else if (locationRemote.getIlluminanceState().getIlluminance() < MINIMUM_NEEDED_ILLUMINATION) {
                    regulateShutterLevelUp();
                }
            } else {
                actionRescheduleHelper.stopExecution();
            }
        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            logger.debug("Initializing IlluminationRollerShutterAgent[" + config.getLabel() + "]");
            CachedUnitRegistryRemote.waitForData();

            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("IlluminationRollerShutterAgent", config.getMetaConfig());

            int minimumNeededMeta = -1;
            int maximumWantedMeta = -1;
            try {
                minimumNeededMeta = Integer.parseInt(configVariableProvider.getValue(MINIMUM_NEEDED_KEY));
            } catch (CouldNotPerformException ex) {
            }
            try {
                maximumWantedMeta = Integer.parseInt(configVariableProvider.getValue(MAXIMUM_WANTED_KEY));
            } catch (CouldNotPerformException ex) {
            }
            if (minimumNeededMeta != -1) {
                MINIMUM_NEEDED_ILLUMINATION = minimumNeededMeta;
            }
            if (maximumWantedMeta != -1) {
                MAXIMUM_WANTED_ILLUMINATION = maximumWantedMeta;
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }

        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);
        } catch (NotAvailableException ex) {
            throw new InitializationException("LocationRemote not available.", ex);
        }

        try {
            IlluminanceDualBoundaryTrigger<LocationRemote, LocationDataType.LocationData> agentTrigger = new IlluminanceDualBoundaryTrigger(locationRemote, MAXIMUM_WANTED_ILLUMINATION, MINIMUM_NEEDED_ILLUMINATION, IlluminanceDualBoundaryTrigger.TriggerOperation.OUTSIDE_ACTIVE);
            agentTriggerHolder.addTrigger(agentTrigger, TriggerPool.TriggerOperation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not add agent to agentpool", ex);
        }
    }

    private void regulateShutterLevelDown() {
        try {
            ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
                    ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
                    1000 * 30,
                    ResourceAllocationType.ResourceAllocation.Policy.FIRST,
                    ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
                    locationRemote,
                    BlindStateType.BlindState.newBuilder().setMovementState(BlindStateType.BlindState.MovementState.DOWN).build(),
                    UnitTemplateType.UnitTemplate.UnitType.UNKNOWN,
                    ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE,
                    MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
            actionRescheduleHelper.startActionRescheduleing(locationRemote.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            logger.error("Could not dim lights.", ex);
        }
    }

    private void regulateShutterLevelUp() {
        try {
            ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = getNewActionDescription(ActionAuthorityType.ActionAuthority.getDefaultInstance(),
                    ResourceAllocationType.ResourceAllocation.Initiator.SYSTEM,
                    1000 * 30,
                    ResourceAllocationType.ResourceAllocation.Policy.FIRST,
                    ResourceAllocationType.ResourceAllocation.Priority.NORMAL,
                    locationRemote,
                    BlindStateType.BlindState.newBuilder().setMovementState(BlindStateType.BlindState.MovementState.UP).build(),
                    UnitTemplateType.UnitTemplate.UnitType.UNKNOWN,
                    ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE,
                    MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE);
            actionRescheduleHelper.startActionRescheduleing(locationRemote.applyAction(actionDescriptionBuilder.build()).get().toBuilder());
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            logger.error("Could not dim lights.", ex);
        }
    }
}
