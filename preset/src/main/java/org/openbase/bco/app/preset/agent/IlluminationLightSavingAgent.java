package org.openbase.bco.app.preset.agent;

/*
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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
import org.openbase.bco.dal.remote.layer.unit.unitgroup.UnitGroupRemote;
import org.openbase.bco.dal.remote.trigger.GenericDualBoundedDoubleValueTrigger;
import org.openbase.bco.dal.remote.trigger.preset.IlluminanceDualBoundaryTrigger;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.trigger.TriggerPool;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.location.LocationDataType;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class IlluminationLightSavingAgent extends AbstractTriggerableAgent {

    private static final int SLEEP_MILLI = 5000;
    public static final String MINIMUM_NEEDED_KEY = "MINIMUM_ILLUMINATION";
    public static final String MAXIMUM_WANTED_KEY = "MAXIMUM_ILLUMINATION";
    private static double MINIMUM_NEEDED_ILLUMINATION = 2000;
    private static double MAXIMUM_WANTED_ILLUMINATION = 4000;

    private LocationRemote locationRemote;
    private ActionDescription taskActionDescription;

    public IlluminationLightSavingAgent() throws InstantiationException {
        super();
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            logger.debug("Initializing IlluminationLightSavingAgent[" + LabelProcessor.getBestMatch(config.getLabel()) + "]");
            CachedUnitRegistryRemote.waitForData();

            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("IlluminationLightSavingAgent", config.getMetaConfig());

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
            registerTrigger(new IlluminanceDualBoundaryTrigger<LocationRemote, LocationDataType.LocationData>(locationRemote, MAXIMUM_WANTED_ILLUMINATION, MINIMUM_NEEDED_ILLUMINATION, GenericDualBoundedDoubleValueTrigger.TriggerOperation.HIGH_ACTIVE), TriggerPool.TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void regulateLightIntensity() throws CouldNotPerformException, ExecutionException, InterruptedException{
        // If AmbientLightGroup is defined, turn all of them off
        List<? extends UnitGroupRemote> unitsByLabel = Units.getUnitsByLabel(locationRemote.getLabel().concat("AmbientLightGroup"), true, Units.UNITGROUP);
        if (!unitsByLabel.isEmpty()) {
            UnitGroupRemote ambientLightGroup = unitsByLabel.get(0);
            taskActionDescription = ambientLightGroup.applyAction(generateAction(UnitType.UNKNOWN,
                    ServiceType.POWER_STATE_SERVICE,
                    PowerState.newBuilder().setValue(PowerState.State.OFF)).setExecutionTimePeriod(Long.MAX_VALUE).build()).get();
            // Wait for a bit, so the system can recognize the change in illuminance after turning off AmbientLightGroup
            Thread.sleep(SLEEP_MILLI);
        }
        if (locationRemote.getIlluminanceState().getIlluminance() > MAXIMUM_WANTED_ILLUMINATION) {
            if(taskActionDescription != null) {
                taskActionDescription = locationRemote.cancelAction(taskActionDescription).get();
            }
            taskActionDescription = locationRemote.applyAction(generateAction(UnitType.UNKNOWN,
                    ServiceType.POWER_STATE_SERVICE,
                    PowerState.newBuilder().setValue(PowerState.State.OFF)).setExecutionTimePeriod(Long.MAX_VALUE).build()).get();

        }
    }

    @Override
    protected void trigger(ActivationStateType.ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (activationState.getValue()) {
            case ACTIVE:
                regulateLightIntensity();
              break;
            case DEACTIVE:
                if(taskActionDescription != null) {
                    taskActionDescription = locationRemote.cancelAction(taskActionDescription).get();
                }
                break;
        }
    }
}
