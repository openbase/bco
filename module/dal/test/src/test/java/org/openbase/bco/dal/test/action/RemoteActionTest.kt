package org.openbase.bco.dal.test.action

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.authentication.lib.SessionManager
import org.openbase.bco.dal.remote.action.RemoteAction
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.remote.session.TokenGenerator
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin
import org.openbase.bco.registry.unit.lib.UnitRegistry
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription
import org.openbase.type.domotic.action.ActionParameterType
import org.openbase.type.domotic.action.ActionPriorityType
import org.openbase.type.domotic.authentication.AuthTokenType
import org.openbase.type.domotic.state.ActionStateType
import org.openbase.type.domotic.state.PowerStateType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitTemplateType
import org.slf4j.LoggerFactory
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/*-
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
class RemoteActionTest : AbstractBCOLocationManagerTest() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RemoteActionTest::class.java)
        private var mrPinkUserToken: AuthTokenType.AuthToken? = null
        private var mrPinkUserId: String? = null
        private var mrPinkActionParameter: ActionParameterType.ActionParameter? = null
    }

    @BeforeAll
    @Timeout(30)
    fun setupRemoteActionTest() {
        // create new user token for test
        try {
            // login as admin
            val sessionManager = SessionManager()
            sessionManager.loginUser(
                Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).id,
                UserCreationPlugin.ADMIN_PASSWORD,
                false
            )

            // register a new user which is not an admin
            val username = "Undercover"
            val password = "Agent"
            val userUnitConfig =
                UnitConfigType.UnitConfig.newBuilder().setUnitType(UnitTemplateType.UnitTemplate.UnitType.USER)
            userUnitConfig.userConfigBuilder.setFirstName("Mr").setLastName("Pink").userName = username
            mrPinkUserId = Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get().id
            sessionManager.registerUser(mrPinkUserId, password, false).get()

            // login new user
            sessionManager.logout()
            sessionManager.loginUser(mrPinkUserId, password, false)

            // request authentication token for new user
            mrPinkUserToken = TokenGenerator.generateAuthToken(sessionManager)
            mrPinkActionParameter =
                ActionParameterType.ActionParameter.newBuilder().setAuthToken(mrPinkUserToken).build()

            // logout user
            sessionManager.logout()
        } catch (ex: Throwable) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER)
        }
    }

    @Test
    @Timeout(10)
    @Throws(Exception::class)
    fun testExecutionAndCancellationWithToken() {
        println("testExecutionAndCancellationWithToken")
        val locationRemote = Units.getUnit(Registries.getUnitRegistry().rootLocationConfig, true, Units.LOCATION)
        val units =
            locationRemote.getUnits(UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT)
        for (i in 0..9) {
            val powerState = if (i % 2 == 0) PowerStateType.PowerState.State.ON else PowerStateType.PowerState.State.OFF
            val locationRemoteAction = waitForExecution(
                locationRemote.setPowerState(
                    powerState, UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT, mrPinkActionParameter
                ), mrPinkUserToken
            )
            Assertions.assertTrue(
                locationRemoteAction.id.isNotEmpty(), "Action of location does not offer an id after submission!"
            )

            for (unit in units) {
                var actionIsExecuting = false
                for (actionReference in unit.actionList[0].actionCauseList) {
                    Assertions.assertTrue(
                        !actionReference.actionId.isEmpty(),
                        "Subaction of location does not offer an id after submission!"
                    )
                    if (actionReference.actionId == locationRemoteAction.id) {
                        actionIsExecuting = true
                        break
                    }
                }
                Assertions.assertTrue(actionIsExecuting, "Action on unit[$unit] is not executing")
                Assertions.assertEquals(
                    mrPinkUserId,
                    unit.actionList[0].actionInitiator.authenticatedBy,
                    "Action was not authenticated by the correct user"
                )
            }
            locationRemoteAction.cancel().get()

            // validate that action is cancelled on all units
            for (colorableLightRemote in units) {
//                System.out.println("process: " + colorableLightRemote);
                var causedAction: ActionDescription? = null
                for (description in colorableLightRemote.actionList) {
//                    System.out.println("    action: " + ActionDescriptionProcessor.toString(description));
                    for (cause in description.actionCauseList) {
//                        System.out.println("        reference: " + ActionDescriptionProcessor.toString(cause));
//                        System.out.println("        compare  : " + locationRemoteAction.getId());
                        if (cause.actionId == locationRemoteAction.id) {
                            causedAction = description
                            //                            System.out.println("            match: " + ActionDescriptionProcessor.toString(causedAction));
                            break
                        }
                    }
                }
                if (causedAction == null) {
                    Assertions.fail<Any>("Caused action on unit[$colorableLightRemote] could not be found!")
                }
                Assertions.assertEquals(
                    ActionStateType.ActionState.State.CANCELED,
                    causedAction!!.actionState.value,
                    "Action on unit[$colorableLightRemote] was not cancelled!"
                )
            }
        }
    }

    @Test
    @Timeout(15)
    @Throws(Exception::class)
    fun testExtensionCancellation() {
        println("testExtensionCancellation")
        val locationRemote = Units.getUnit(Registries.getUnitRegistry().rootLocationConfig, true, Units.LOCATION)

        // apply low prio action
        println("apply low prio action...")
        val lowPrioLongtermActionExtentionFlag = Flag()
        val lowPrioLongtermAction = RemoteAction(
            locationRemote.setPowerState(
                PowerStateType.PowerState.State.OFF, ActionParameterType.ActionParameter.newBuilder().setPriority(
                    ActionPriorityType.ActionPriority.Priority.LOW
                ).setSchedulable(true).setInterruptible(true).setExecutionTimePeriod(
                    TimeUnit.MINUTES.toMicros(30)
                ).build()
            )
        ) {
            println("low prio action is extended")
            lowPrioLongtermActionExtentionFlag.value = true
            true
        }
        observe(lowPrioLongtermAction)
        println("wait for low prio action...")
        waitForRegistration(lowPrioLongtermAction)
        println("apply normal prio action...")
        val dominantActionExtentionFlag = Flag()
        val dominantAction = RemoteAction(
            locationRemote.setPowerState(PowerStateType.PowerState.State.ON, mrPinkActionParameter), mrPinkUserToken
        ) {
            println("dominant action is extended")
            dominantActionExtentionFlag.value = true
            true

        }
        println("wait for normal prio action...")
        waitForExecution(dominantAction)
        println("validate light state ON")
        val units =
            locationRemote.getUnits(UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT)
        for (unit in units) {
            unit.requestData().get()
            Assertions.assertEquals(PowerStateType.PowerState.State.ON, unit.powerState.value, "Light[$unit] not on")
        }
        println("cancel dominant action")
        dominantAction.cancel().get()
        dominantActionExtentionFlag.value = false
        println("validate light state OFF")
        lowPrioLongtermAction.waitForActionState(ActionStateType.ActionState.State.EXECUTING)
        for (unit in units) {
            unit.requestData().get()
            Assertions.assertEquals(PowerStateType.PowerState.State.OFF, unit.powerState.value, "Light[$unit] not off")
        }
        println("wait until last extension timeout and validate if no further extension will be performed for dominant action...")
        Thread.sleep(dominantAction.getValidityTime(TimeUnit.MILLISECONDS) + 10)
        Assertions.assertFalse(
            dominantActionExtentionFlag.value,
            "Dominant action was extended even through the action was already canceled."
        )
        println("cancel low prio action")
        lowPrioLongtermAction.cancel().get()
        lowPrioLongtermActionExtentionFlag.value = false
        println("validate everything is done")
        for (unit in units) {
            unit.requestData().get()
            for (actionDescription in unit.actionList) {

                // filter termination action
                if (actionDescription.priority == ActionPriorityType.ActionPriority.Priority.TERMINATION) {
                    continue
                }
                Assertions.assertTrue(
                    RemoteAction(actionDescription).isDone,
                    "Zombie[" + actionDescription.actionState.value.name + "] actions detected: " + MultiLanguageTextProcessor.getBestMatch(
                        actionDescription.description
                    )
                )
            }
        }
        println("wait until last extension timeout and validate if no further extension will be performed for low prio longterm action...")
        Thread.sleep(lowPrioLongtermAction.getValidityTime(TimeUnit.MILLISECONDS) + 10)
        Assertions.assertEquals(
            false,
            lowPrioLongtermActionExtentionFlag.value,
            "Low prio action was extended even through the action was already canceled."
        )
        println("validate if still everything is done")
        for (unit in units) {
            for (actionDescription in unit.actionList) {

                // filter termination action
                if (actionDescription.priority == ActionPriorityType.ActionPriority.Priority.TERMINATION) {
                    continue
                }
                Assertions.assertEquals(
                    true,
                    RemoteAction(actionDescription).isDone,
                    "Zombie[" + actionDescription.actionState.value.name + "] actions detected: " + MultiLanguageTextProcessor.getBestMatch(
                        actionDescription.description
                    )
                )
            }
        }
        println("test successful")
    }

    @Test
    fun `should not fail on cancel if action was never executed`() {

        val failedServiceCall = CompletableFuture.failedFuture<ActionDescription>(CancellationException())
        RemoteAction(failedServiceCall).cancel().get()
    }


    internal data class Flag(
        @Volatile var value: Boolean = false,
    )
}
