package org.openbase.bco.app.preset.agent

import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote
import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger
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
import org.openbase.type.domotic.state.ActivationStateType.ActivationState
import org.openbase.type.domotic.state.PresenceStateType
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
class PresenceLightSceneAgent : AbstractTriggerableAgent() {
    private var presenceScene: SceneRemote? = null

    @Throws(InitializationException::class, InterruptedException::class)
    override fun init(config: UnitConfigType.UnitConfig) {
        super.init(config)
        try {
            val locationRemote = Units.getUnit(getConfig().placementConfig.locationId, false, Units.LOCATION)
                ?: throw VerificationFailedException("Configured target location not found!")

            // activation trigger
            registerActivationTrigger(
                GenericServiceStateValueTrigger<LocationRemote, LocationData, PresenceStateType.PresenceState.State>(
                    locationRemote,
                    PresenceStateType.PresenceState.State.PRESENT,
                    ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE
                ), TriggerAggregation.AND
            )
            registerActivationTrigger(
                GenericBoundedDoubleValueTrigger<LocationRemote, LocationData>(
                    locationRemote,
                    MIN_ILLUMINANCE_UNTIL_TRIGGER,
                    GenericBoundedDoubleValueTrigger.TriggerOperation.LOW_ACTIVE,
                    ServiceTemplateType.ServiceTemplate.ServiceType.ILLUMINANCE_STATE_SERVICE,
                    "getIlluminance"
                ), TriggerAggregation.AND
            )

            // deactivation trigger
            registerDeactivationTrigger(
                GenericServiceStateValueTrigger<LocationRemote, LocationData, PresenceStateType.PresenceState.State>(
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
            val relatedSceneAlias = getConfig().getAlias(0) + "_" + PRESENCE_LIGHT_SCENE_PREFIX

            // legacy handling to update outdated aliases
            try {
                val outDatedScene = Registries.getUnitRegistry().getUnitConfigByAliasAndUnitType(
                    PRESENCE_LIGHT_SCENE_PREFIX, UnitTemplateType.UnitTemplate.UnitType.SCENE
                ).toBuilder()

                // update alias
                val aliases = ArrayList(outDatedScene.aliasList)
                aliases.remove(PRESENCE_LIGHT_SCENE_PREFIX)
                aliases.add(relatedSceneAlias)
                outDatedScene.clearAlias()
                outDatedScene.addAllAlias(aliases)

                // save
                try {
                    Registries.getUnitRegistry().updateUnitConfig(outDatedScene.build())[5, TimeUnit.SECONDS]
                } catch (exx: ExecutionException) {
                    ExceptionPrinter.printHistory<Exception>("Could not register Presence Light Group", exx, logger)
                } catch (exx: TimeoutException) {
                    ExceptionPrinter.printHistory<Exception>("Could not register Presence Light Group", exx, logger)
                }
            } catch (ex: NotAvailableException) {
                // this should be the normal case so just continue...
            }

            // create presence scene if not available otherwise load config of existing one
            var presenceLightSceneConfig: UnitConfigType.UnitConfig? = null
            try {
                presenceLightSceneConfig = Registries.getUnitRegistry()
                    .getUnitConfigByAliasAndUnitType(relatedSceneAlias, UnitTemplateType.UnitTemplate.UnitType.SCENE)
            } catch (ex: NotAvailableException) {
                val presenceLightSceneBuilder = UnitConfigType.UnitConfig.newBuilder()
                presenceLightSceneBuilder.addAlias(relatedSceneAlias)
                presenceLightSceneBuilder.unitType = UnitTemplateType.UnitTemplate.UnitType.SCENE
                addLabel(presenceLightSceneBuilder.labelBuilder, Locale.ENGLISH, "Presence Light Scene")
                addLabel(presenceLightSceneBuilder.labelBuilder, Locale.GERMAN, "Anwesenheitslicht Scene")
                try {
                    // save
                    presenceLightSceneConfig = Registries.getUnitRegistry()
                        .registerUnitConfig(presenceLightSceneBuilder.build())[5, TimeUnit.SECONDS]
                } catch (exx: ExecutionException) {
                    ExceptionPrinter.printHistory("Could not register Presence Light Group", ex, logger)
                } catch (exx: TimeoutException) {
                    ExceptionPrinter.printHistory("Could not register Presence Light Group", ex, logger)
                }
            }

            // load presence scene
            presenceScene = Units.getUnit(presenceLightSceneConfig, false, Units.SCENE)
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

        // activate presence scene
        when (activationState.value) {
            ActivationState.State.ACTIVE -> observe(
                presenceScene?.setActivationState(
                    ActivationState.State.ACTIVE,
                    getDefaultActionParameter(Timeout.INFINITY_TIMEOUT)
                )
            )

            else -> cancelAllObservedActions()
        }
    }

    companion object {
        const val PRESENCE_LIGHT_SCENE_PREFIX = "PresenceLightScene"
        const val MIN_ILLUMINANCE_UNTIL_TRIGGER = 100.0
    }
}
