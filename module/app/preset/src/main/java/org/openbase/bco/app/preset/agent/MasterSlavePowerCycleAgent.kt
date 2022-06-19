package org.openbase.bco.app.preset.agent

import org.openbase.bco.app.preset.agent.AbstractTriggerableAgent
import org.openbase.bco.dal.remote.layer.unit.PowerConsumptionSensorRemote
import org.openbase.bco.dal.remote.layer.service.PowerStateServiceRemote
import org.openbase.bco.dal.remote.layer.unit.Units
import java.lang.InterruptedException
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider
import org.openbase.jul.exception.NotAvailableException
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger.TriggerOperation.HIGH_ACTIVE
import org.openbase.type.domotic.unit.dal.PowerConsumptionSensorDataType.PowerConsumptionSensorData
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InitializationException
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation.OR
import org.openbase.jul.schedule.Timeout
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE
import org.openbase.type.domotic.state.ActivationStateType.ActivationState
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State.ACTIVE
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State.INACTIVE
import org.openbase.type.domotic.state.PowerStateType.PowerState
import org.openbase.type.domotic.state.PowerStateType.PowerState.State.ON
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

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
 */ /**
 * @author [Timo Michalski](mailto:tmichalski@techfak.uni-bielefeld.de)
 */
class MasterSlavePowerCycleAgent : AbstractTriggerableAgent() {
    private var master: PowerConsumptionSensorRemote? = null
    private var slave: PowerStateServiceRemote? = null
    @Throws(InitializationException::class, InterruptedException::class)
    override fun init(config: UnitConfig) {
        super.init(config)
        try {
            val variableProvider = MetaConfigVariableProvider("AgentConfig", config.metaConfig)
            if (master != null) {
                master!!.shutdown()
            }
            if (slave != null) {
                slave!!.shutdown()
            }
            slave = PowerStateServiceRemote()

            // resolve master
            master = try {
                Units.getUnit(variableProvider.getValue("MASTER_ID"), false, Units.POWER_CONSUMPTION_SENSOR)
            } catch (ex: NotAvailableException) {
                Units.getUnitByAlias(variableProvider.getValue("MASTER_ALIAS"), false, Units.POWER_CONSUMPTION_SENSOR)
            }

            // resolve master
            try {
                slave!!.init(Registries.getUnitRegistry(true).getUnitConfigById(variableProvider.getValue("SLAVE_ID")))
            } catch (ex: NotAvailableException) {
                slave!!.init(
                    Registries.getUnitRegistry(true).getUnitConfigByAlias(variableProvider.getValue("SLAVE_ALIAS"))
                )
            }

            // activation trigger
            registerActivationTrigger(
                GenericBoundedDoubleValueTrigger(
                    master,
                    variableProvider.getValue("HIGH_ACTIVE_POWER_THRESHOLD", "1.0").toDouble(),
                    HIGH_ACTIVE,
                    POWER_CONSUMPTION_STATE_SERVICE,
                    "getConsumption"
                ), OR
            )
        } catch (ex: CouldNotPerformException) {
            throw InitializationException(this, ex)
        }
    }

    @Throws(
        CouldNotPerformException::class,
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    override fun trigger(activationState: ActivationState) {

        // sync slave
        when (activationState.value) {
            ACTIVE -> observe(slave!!.setPowerState(ON, getDefaultActionParameter(Timeout.INFINITY_TIMEOUT)))
            INACTIVE -> cancelAllObservedActions()
        }
    }
}
