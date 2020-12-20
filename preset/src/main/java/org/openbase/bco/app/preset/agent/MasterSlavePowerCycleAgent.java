package org.openbase.bco.app.preset.agent;

/*
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

import org.openbase.bco.dal.remote.layer.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.layer.unit.PowerConsumptionSensorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger;
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger.TriggerOperation;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class MasterSlavePowerCycleAgent extends AbstractTriggerableAgent {

    public final static String XMAS_SCENE = "PresenceScene";

    public static final double MIN_ILLUMINANCE_UNTIL_TRIGGER = 100d;

    private LocationRemote locationRemote;

    private PowerConsumptionSensorRemote master;
    private PowerStateServiceRemote slave;

    public MasterSlavePowerCycleAgent() throws InstantiationException {
        super();
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), false, Units.LOCATION);


            final MetaConfigVariableProvider variableProvider = new MetaConfigVariableProvider("AgentConfig", config.getMetaConfig());

            if (master != null) {
                master.shutdown();
            }

            if (slave != null) {
                slave.shutdown();
            }

            slave = new PowerStateServiceRemote();

            // resolve master
            try {
                master = Units.getUnit(variableProvider.getValue("MASTER_ID"), false, Units.POWER_CONSUMPTION_SENSOR);
            } catch (NotAvailableException ex) {
                master = Units.getUnitByAlias(variableProvider.getValue("MASTER_ALIAS"), false, Units.POWER_CONSUMPTION_SENSOR);
            }

            // resolve master
            try {
                slave.init(Registries.getUnitRegistry(true).getUnitConfigById(variableProvider.getValue("SLAVE_ID")));
            } catch (NotAvailableException ex) {
                slave.init(Registries.getUnitRegistry(true).getUnitConfigByAlias(variableProvider.getValue("SLAVE_ALIAS")));
            }

            // activation trigger
            registerActivationTrigger(new GenericBoundedDoubleValueTrigger<>(master, Double.parseDouble(variableProvider.getValue("HIGH_ACTIVE_POWER_THRESHOLD")), TriggerOperation.HIGH_ACTIVE, ServiceType.POWER_CONSUMPTION_STATE_SERVICE, "getConsumption"), TriggerAggregation.OR);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void trigger(final ActivationState activationState) throws
            CouldNotPerformException, ExecutionException, InterruptedException, TimeoutException {

        // activate xmas scene
        switch (activationState.getValue()) {
            case ACTIVE:
                observe(slave.setPowerState(State.ON, getDefaultActionParameter(Timeout.INFINITY_TIMEOUT)));
                break;
            case INACTIVE:
                cancelAllObservedActions();
                break;
        }
    }
}
