package org.openbase.jul.communication.controller;

/*-
 * #%L
 * JUL Extension Controller
 * %%
 * Copyright (C) 2015 - 2022 openbase.org
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.openbase.jul.communication.mqtt.SharedMqttClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

public class MqttIntegrationTest {

    public static final int port = 1883;
    public static Path mosquittoConfig;
    public static GenericContainer<?> broker;

    @BeforeAll
    public static void setUpClass() throws JPServiceException, IOException {
        mosquittoConfig = Files.createTempFile("mosquitto_", ".conf");
        Files.write(mosquittoConfig, Arrays.asList(
                "allow_anonymous true",
                "listener "+port)
        );

        broker = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto"))
                .withExposedPorts(port)
                .withFileSystemBind(
                        mosquittoConfig.toString(),
                        "/mosquitto/config/mosquitto.conf",
                        BindMode.READ_ONLY
                );
        broker.withStartupTimeout(Duration.ofSeconds(30)).start();

        JPService.registerProperty(JPComPort.class, broker.getFirstMappedPort());
        JPService.registerProperty(JPComHost.class, broker.getHost());
        JPService.setupJUnitTestMode();
    }

    @AfterAll
    public static void tearDownClass() throws IOException {
        SharedMqttClient.INSTANCE.waitForShutdown();
        broker.stop();
        Files.delete(mosquittoConfig);
    }
}
