package org.openbase.bco.authentication.mock

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.openbase.jps.core.JPService
import org.openbase.jps.exception.JPServiceException
import org.openbase.jul.communication.jp.JPComHost
import org.openbase.jul.communication.jp.JPComPort
import org.openbase.jul.communication.mqtt.SharedMqttClient.waitForShutdown
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.*

/*-
 * #%L
 * JUL Extension Controller
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

 * */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class MqttIntegrationTest {

    companion object {
        const val port = 1884
        var mosquittoConfig: Path? = null
        var broker: GenericContainer<*>? = null
        val configLock = Any()
    }

    @BeforeAll
    fun setupMqtt() {
        synchronized(configLock) {
            mosquittoConfig = Files.createTempFile("mosquitto_", ".conf")
            Files.write(
                mosquittoConfig, listOf(
                    "allow_anonymous true",
                    "listener " + port
                )
            )
            GenericContainer(DockerImageName.parse("eclipse-mosquitto"))
                .withExposedPorts(port)
                .withFileSystemBind(
                    mosquittoConfig.toString(),
                    "/mosquitto/config/mosquitto.conf",
                    BindMode.READ_ONLY
                )
                .apply { withStartupTimeout(Duration.ofSeconds(30)).start() }
                .also { broker?.containerId?.also { error("broker was already initialized!") } }
                .also { broker = it }
                .also { setupProperties() }
        }
    }

    @AfterAll
    fun tearDownMQTT() {
        synchronized(configLock) {
            waitForShutdown()
            broker?.stop()
            Files.delete(mosquittoConfig)
        }
    }

    @Throws(JPServiceException::class)
    private fun setupProperties() {
        JPService.reset()
        JPService.registerProperty(JPComPort::class.java, broker!!.firstMappedPort)
        JPService.registerProperty(JPComHost::class.java, broker!!.host)
        setupCustomProperties()
        JPService.setupJUnitTestMode()
    }

    open fun setupCustomProperties() {}
}
