package org.openbase.bco.app.preset.agent

import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.jul.schedule.Timeout
import org.openbase.type.domotic.state.ActivationStateType.ActivationState
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class AbstractSceneAgent : AbstractTriggerableAgent() {
    private var scenes: List<SceneRemote> = emptyList()

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun applyConfigUpdate(config: UnitConfig): UnitConfig = getManageWriteLockInterruptible(this).use {
        super.applyConfigUpdate(config).also { unitConfig ->

            // create agent scene if not available otherwise load config of existing one
            scenes = try {
                Registries.getUnitRegistry().getBindings(unitConfig.id, listOf(UnitType.SCENE))
            } catch (ex: NotAvailableException) {
                UnitConfig.newBuilder()
                    .apply {
                        addBindingId(unitConfig.id)
                        unitType = UnitType.SCENE
                        LabelProcessor.addLabel(
                            labelBuilder,
                            Locale.ENGLISH,
                            LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.label)
                        )
                        LabelProcessor.addLabel(
                            labelBuilder,
                            Locale.GERMAN,
                            LabelProcessor.getBestMatch(Locale.GERMAN, unitConfig.label)
                        )
                        LabelProcessor.addLabel(labelBuilder, Locale.GERMAN, "Anwesenheitslicht Scene")
                    }.let { sceneConfig ->
                        try {
                            Registries.getUnitRegistry()
                                .registerUnitConfig(sceneConfig.build())[5, TimeUnit.SECONDS]
                        } catch (exx: ExecutionException) {
                            ExceptionPrinter.printHistory("Could not register Presence Light Group", ex, logger)
                            null
                        } catch (exx: TimeoutException) {
                            ExceptionPrinter.printHistory("Could not register Presence Light Group", ex, logger)
                            null
                        }
                    }.let { it -> listOf(it) }
            }.map { Units.getUnit(it, false, Units.SCENE) }
        }
    }

    override fun trigger(activationState: ActivationState) {
        // activate presence scene
        when (activationState.value) {
            ActivationState.State.ACTIVE -> scenes
                .map { scene ->
                    scene.setActivationState(
                        ActivationState.State.ACTIVE,
                        getDefaultActionParameter(Timeout.INFINITY_TIMEOUT)
                    )
                }.forEach { observe(it) }

            else -> cancelAllObservedActions()
        }
    }
}
