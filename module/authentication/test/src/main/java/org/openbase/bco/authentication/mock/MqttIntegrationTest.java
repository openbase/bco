package org.openbase.bco.authentication.mock;

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
 */

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class MqttIntegrationTest {

    public static final int port = 1883;
    public static final int httpPort = 8888;

    @ClassRule
    public static GenericContainer broker = new GenericContainer(DockerImageName.parse("vernemq/vernemq"))
            .withEnv("DOCKER_VERNEMQ_ACCEPT_EULA", "yes")
            .withEnv("DOCKER_VERNEMQ_LISTENER.tcp.allowed_protocol_versions", "5") // enable mqtt5
            .withEnv("DOCKER_VERNEMQ_ALLOW_ANONYMOUS", "on") // enable connection without password
            .withExposedPorts(port, httpPort)
            .waitingFor(Wait.forHttp("/health").forPort(httpPort).forStatusCode(200));

    @BeforeClass
    public static void setUpClass() throws JPServiceException, InterruptedException {
        JPService.registerProperty(JPComPort.class, broker.getFirstMappedPort());
        JPService.registerProperty(JPComHost.class, broker.getHost());
        JPService.setupJUnitTestMode();
    }
}
