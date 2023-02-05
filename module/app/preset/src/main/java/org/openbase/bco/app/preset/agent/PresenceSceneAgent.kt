package org.openbase.bco.app.preset.agent

import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote
import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote
import org.openbase.bco.dal.remote.trigger.GenericServiceStateValueTrigger
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InitializationException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.exception.VerificationFailedException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.LabelProcessor.addLabel
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation
import org.openbase.jul.schedule.Timeout
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.state.ActivationStateType
import org.openbase.type.domotic.state.PresenceStateType
import org.openbase.type.domotic.state.PresenceStateType.PresenceState
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitTemplateType
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
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
class PresenceSceneAgent : AbstractTriggerableAgent() {
    private var presenceScene: SceneRemote? = null

    @Throws(InitializationException::class, InterruptedException::class)
    override fun init(config: UnitConfigType.UnitConfig) {
        super.init(config)
        try {
            val locationRemote = Units.getUnit(getConfig().placementConfig.locationId, false, Units.LOCATION)
                ?: throw VerificationFailedException("Configured target location not found!")

            // activation trigger
            registerActivationTrigger(
                GenericServiceStateValueTrigger<LocationRemote, LocationData, PresenceState.State>(
                    locationRemote,
                    PresenceStateType.PresenceState.State.PRESENT,
                    ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE
                ), TriggerAggregation.OR
            )

            // deactivation trigger
            registerDeactivationTrigger(
                GenericServiceStateValueTrigger<LocationRemote, LocationData, PresenceState.State>(
                    locationRemote,
                    PresenceStateType.PresenceState.State.ABSENT,
                    ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE
                ), TriggerAggregation.OR
            )
        } catch (ex: CouldNotPerformException) {
            throw InitializationException(this, ex)
        }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun applyConfigUpdate(config: UnitConfigType.UnitConfig): UnitConfigType.UnitConfig {
        getManageWriteLockInterruptible(this).use { ignored ->
            val unitConfig = super.applyConfigUpdate(config)

            // create xmas scene if not available otherwise load config of existing one
            var xMasLightSceneConfig: UnitConfigType.UnitConfig? = null
            try {
                xMasLightSceneConfig = Registries.getUnitRegistry()
                    .getUnitConfigByAliasAndUnitType(XMAS_SCENE, UnitTemplateType.UnitTemplate.UnitType.SCENE)
            } catch (ex: NotAvailableException) {
                val presenceLightSceneBuilder = UnitConfigType.UnitConfig.newBuilder()
                presenceLightSceneBuilder.unitType = UnitTemplateType.UnitTemplate.UnitType.SCENE
                presenceLightSceneBuilder.addAlias(XMAS_SCENE)
                addLabel(presenceLightSceneBuilder.labelBuilder, Locale.ENGLISH, "Presence Scene")
                addLabel(presenceLightSceneBuilder.labelBuilder, Locale.GERMAN, "Anwesenheits Scene")
                try {
                    xMasLightSceneConfig = Registries.getUnitRegistry()
                        .registerUnitConfig(presenceLightSceneBuilder.build())[5, TimeUnit.SECONDS]
                } catch (exx: ExecutionException) {
                    ExceptionPrinter.printHistory("Could not register XMas Light Group", ex, logger)
                } catch (exx: TimeoutException) {
                    ExceptionPrinter.printHistory("Could not register XMas Light Group", ex, logger)
                }
            }

            // load xmas scene
            presenceScene = Units.getUnit(xMasLightSceneConfig, false, Units.SCENE)
            return unitConfig
        }
    }

    @Throws(
        CouldNotPerformException::class,
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    override fun trigger(activationState: ActivationStateType.ActivationState) {

        // activate xmas scene
        when (activationState.value) {
            ActivationStateType.ActivationState.State.ACTIVE -> observe(
                presenceScene!!.setActivationState(
                    ActivationStateType.ActivationState.State.ACTIVE, getDefaultActionParameter(
                        Timeout.INFINITY_TIMEOUT
                    )
                )
            )

            else -> cancelAllObservedActions()
        }
    }

    companion object {
        const val XMAS_SCENE = "PresenceScene"
    }
}
