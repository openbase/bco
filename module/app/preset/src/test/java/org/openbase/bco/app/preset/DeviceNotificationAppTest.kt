package org.openbase.bco.app.preset

import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.Test
import org.openbase.app.test.agent.AbstractBCOAppManagerTest
import org.openbase.bco.dal.control.layer.unit.BatteryController
import org.openbase.bco.dal.lib.layer.unit.Unit
import org.openbase.bco.dal.lib.state.States
import org.openbase.bco.dal.remote.layer.unit.BatteryRemote
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter
import org.openbase.bco.registry.mock.MockRegistry
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor
import org.openbase.type.domotic.communication.UserMessageType.UserMessage
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType
import org.openbase.type.domotic.state.ActivationStateType.ActivationState
import org.openbase.type.domotic.state.BatteryStateType.BatteryState
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitTemplateType
import org.openbase.type.domotic.unit.app.AppDataType.AppData
import java.util.*
import java.util.concurrent.TimeUnit

class DeviceNotificationAppTest : AbstractBCOAppManagerTest<DeviceNotificationApp>() {

    private val APP_ALIAS = "Device_Notification_App_Test"
    override fun getAppClass() = DeviceNotificationApp::class.java

    override fun getAppConfig(): UnitConfig.Builder = MockRegistry.generateAppConfig(
        APP_ALIAS,
        MockRegistry.ALIAS_LOCATION_ROOT_PARADISE
    )

    private fun getUserMessagesOfUnit(unit: Unit<*>): List<UserMessage> = run {
        Registries.getMessageRegistry().requestData().get(5, TimeUnit.SECONDS)
    }.let {
        Registries.getMessageRegistry().userMessages
    }.filter { it.conditionList.any { condition -> condition.unitId == unit.id } }

    @Test
    fun testLowBatteryNotification() {
        println("testAbsenceEnergySavingAgent")

        val unitStateAwaiter = UnitStateAwaiter(appRemote)
        unitStateAwaiter.waitForState { data: AppData ->
            data.activationState.getValue() == ActivationState.State.ACTIVE
        }

        val location = Units.getUnitByAlias(MockRegistry.ALIAS_LOCATION_STAIRWAY_TO_HEAVEN, true, Units.LOCATION)
        val batteryRemote: BatteryRemote =
            location.getUnits(UnitTemplateType.UnitTemplate.UnitType.BATTERY, true, Units.BATTERY).first()

        val batteryStateAwaiter = UnitStateAwaiter(batteryRemote)
        val batteryController =
            deviceManagerLauncher.launchable!!.unitControllerRegistry.get(batteryRemote.getId()) as BatteryController

        // verify ground truth
        getUserMessagesOfUnit(batteryRemote) shouldBeEqual emptyList()

        appController!!.checkDevices()
//        messageManagerLauncher.launchable!!.removeOutdatedMessages()
        Thread.sleep(2000)

        // verify unknown battery state notification
        getUserMessagesOfUnit(batteryRemote).size shouldBeEqual 1

        batteryController.applyServiceState(States.Battery.OK, ServiceType.BATTERY_STATE_SERVICE)
        batteryStateAwaiter.waitForState { it.batteryState.value == BatteryState.State.OK }
        batteryRemote.requestData().get(5, TimeUnit.SECONDS)


        appController!!.checkDevices()
//        messageManagerLauncher.launchable!!.removeOutdatedMessages()

        Thread.sleep(2000)

        // verify everything is ok again
        getUserMessagesOfUnit(batteryRemote) shouldBeEqual emptyList()

        batteryController.applyServiceState(States.Battery.CRITICAL, ServiceType.BATTERY_STATE_SERVICE)
        batteryStateAwaiter.waitForState { it.batteryState.value == BatteryState.State.CRITICAL }

        appController!!.checkDevices()
        messageManagerLauncher.launchable!!.removeOutdatedMessages()

        // verify critical battery state notification
        getUserMessagesOfUnit(batteryRemote)
            .also { it.size shouldBeEqual 1 }
            .first().apply {
                messageType shouldBeEqual UserMessage.MessageType.WARNING
                MultiLanguageTextProcessor.getBestMatch(
                    Locale.GERMAN,
                    text
                ) shouldBeEqual "Batteriezustand von F Motion Sensor Device Stairway ist CRITICAL"
                MultiLanguageTextProcessor.getBestMatch(
                    Locale.ENGLISH,
                    text
                ) shouldBeEqual "Battery level of F Motion Sensor Device Stairway is CRITICAL"

            }

        batteryController.applyServiceState(States.Battery.OK, ServiceType.BATTERY_STATE_SERVICE)
        batteryStateAwaiter.waitForState { it.batteryState.value == BatteryState.State.OK }

        appController!!.checkDevices()
        messageManagerLauncher.launchable!!.removeOutdatedMessages()

        // verify all messages are removed after the battery has been replaced.
        getUserMessagesOfUnit(batteryRemote) shouldBeEqual emptyList()
    }
}
