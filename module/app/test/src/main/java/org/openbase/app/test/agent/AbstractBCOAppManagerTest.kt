package org.openbase.app.test.agent

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Timeout
import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.app.AppRemote
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.LabelProcessor.addLabel
import org.openbase.type.domotic.state.ActivationStateType
import org.openbase.type.domotic.state.ConnectionStateType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitTemplateType
import org.openbase.type.domotic.unit.app.AppClassType
import org.openbase.type.domotic.unit.app.AppDataType
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

/*-
* #%L
* BCO App Test Framework
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
*/
abstract class AbstractBCOAppManagerTest<APP_CLASS : AbstractAppController?> : BCOAppTest() {
    protected var appClass: AppClassType.AppClass? = null
    protected var appConfig: UnitConfigType.UnitConfig? = null
    protected var appRemote: AppRemote? = null
    protected var appController: APP_CLASS? = null

    @BeforeEach
    @Timeout(30)
    @Throws(Exception::class)
    fun prepareAppManager() {
        try {
            // setup and register app class
            val appClassBuilder = AppClassType.AppClass.newBuilder()
            addLabel(
                appClassBuilder.getLabelBuilder(),
                Locale.ENGLISH,
                getAppClass().getSimpleName().replace("App", "")
            )
            val appClass = Registries.getClassRegistry().registerAppClass(appClassBuilder.build())[5, TimeUnit.SECONDS]
            val appConfigBuilder = getAppConfig()
            appConfigBuilder.getAppConfigBuilder().setAppClassId(appClass.getId())
            appConfigBuilder.setUnitType(UnitTemplateType.UnitTemplate.UnitType.APP)
            // register app
            val appConfig = Registries.getUnitRegistry()
                .registerUnitConfig(appConfigBuilder.build())[5, TimeUnit.SECONDS]

            Registries.waitUntilReady()

            // retrieve remote and activate app
            val appRemote = Units.getUnit(appConfig, true, Units.APP)
            if (!appConfig.appConfig.autostart) {
                // activate app if not in auto start
                waitForExecution(
                    appRemote.setActivationState(
                        ActivationStateType.ActivationState.newBuilder().setValue(
                            ActivationStateType.ActivationState.State.ACTIVE
                        ).build()
                    )
                )
            } else {
                // wait until active
                UnitStateAwaiter(appRemote).waitForState { data: AppDataType.AppData -> data.activationState.getValue() == ActivationStateType.ActivationState.State.ACTIVE }
            }

            this.appClass = appClass
            this.appConfig = appConfig
            this.appRemote = appRemote
            this.appController =
                appManagerLauncher.launchable!!.appControllerRegistry.get(appConfig?.getId()) as APP_CLASS
        } catch (ex: Exception) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER)
        }
    }

    @AfterEach
    @Timeout(30)
    @Throws(Exception::class)
    fun removeAgent() {
        if (appConfig != null) {
            Registries.getUnitRegistry().removeUnitConfig(appConfig).get()
        }

        if (appRemote != null) {
            appRemote!!.waitForConnectionState(ConnectionStateType.ConnectionState.State.DISCONNECTED)
        }

        if (appClass != null) {
            Registries.getClassRegistry().removeAppClass(appClass).get()
        }
    }

    abstract fun getAppClass(): Class<APP_CLASS>

    @Throws(CouldNotPerformException::class)
    abstract fun getAppConfig(): UnitConfigType.UnitConfig.Builder

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AbstractBCOAgentManagerTest::class.java)
    }
}
