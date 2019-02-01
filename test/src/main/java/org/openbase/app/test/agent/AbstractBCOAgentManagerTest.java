package org.openbase.app.test.agent;

/*-
 * #%L
 * BCO App Test Framework
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openbase.bco.dal.control.layer.unit.agent.AgentManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractBCOAgentManagerTest extends AbstractBCOTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractBCOAgentManagerTest.class);

    protected static AgentManagerLauncher agentManagerLauncher;
    protected static DeviceManagerLauncher deviceManagerLauncher;
    protected static LocationManagerLauncher locationManagerLauncher;

    protected UnitConfig agentConfig = null;
    protected AgentRemote agentRemote = null;

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            LOGGER.info("Start Agent test setup...");
            AbstractBCOTest.setUpClass();

            LOGGER.trace("Start device manager...");
            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch();

            LOGGER.trace("Start agent manager...");
            agentManagerLauncher = new AgentManagerLauncher();
            agentManagerLauncher.launch();

            LOGGER.trace("Start location manager...");
            locationManagerLauncher = new LocationManagerLauncher();
            locationManagerLauncher.launch();

            LOGGER.trace("Finally wait for registry...");
            Registries.waitForData();
            LOGGER.info("Ready to test...");

        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        LOGGER.info("Tear down agent tests...");
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (agentManagerLauncher != null) {
                agentManagerLauncher.shutdown();
            }
            if (locationManagerLauncher != null) {
                locationManagerLauncher.shutdown();
            }
            AbstractBCOTest.tearDownClass();
            LOGGER.info("Agent tests finished.");
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() throws Exception {
        try {
            // register agent
            agentConfig = Registries.getUnitRegistry().registerUnitConfig(getAgentConfig()).get(5, TimeUnit.SECONDS);
            // activate agent
            agentRemote = Units.getUnit(agentConfig, true, Units.AGENT);

            if (!agentConfig.getAgentConfig().getAutostart()) {
                // activate agent if not in auto start
                Actions.waitForExecution(agentRemote.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
            } else {
                // wait until active
                new UnitStateAwaiter<>(agentRemote).waitForState(data -> data.getActivationState().getValue() == State.ACTIVE);
            }
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @After
    public void tearDown() {
    }

    public abstract UnitConfig getAgentConfig() throws CouldNotPerformException;
}
