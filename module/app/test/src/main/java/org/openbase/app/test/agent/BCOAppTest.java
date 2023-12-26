package org.openbase.app.test.agent;

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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.control.layer.unit.agent.AgentManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.app.AppManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.control.message.MessageManagerLauncher;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;

public class BCOAppTest extends AbstractBCOTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractBCOAgentManagerTest.class);

    protected static AgentManagerLauncher agentManagerLauncher;
    protected static AppManagerLauncher appManagerLauncher;
    protected static DeviceManagerLauncher deviceManagerLauncher;
    protected static LocationManagerLauncher locationManagerLauncher;
    protected static MessageManagerLauncher messageManagerLauncher;

    @BeforeAll
    @Timeout(30)
    public static void setupBcoApp() throws Throwable {
        try {
            LOGGER.trace("Start device manager...");
            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch().get();

            LOGGER.trace("Start location manager...");
            locationManagerLauncher = new LocationManagerLauncher();
            locationManagerLauncher.launch().get();

            LOGGER.trace("Start agent manager...");
            agentManagerLauncher = new AgentManagerLauncher();
            agentManagerLauncher.launch().get();

            LOGGER.trace("Start app manager...");
            appManagerLauncher = new AppManagerLauncher();
            appManagerLauncher.launch().get();

            LOGGER.trace("Start message manager...");
            messageManagerLauncher = new MessageManagerLauncher();
            messageManagerLauncher.launch().get();

            LOGGER.trace("Finally wait for registry...");
            Registries.waitForData();

            LOGGER.info("Ready to test...");

            BCOLogin.getSession().loginBCOUser();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterAll
    @Timeout(30)
    public static void tearDownBCOApp() throws Throwable {
        LOGGER.info("Tear down app tests...");
        try {
            if (appManagerLauncher != null) {
                appManagerLauncher.shutdown();
            }
            if (agentManagerLauncher != null) {
                agentManagerLauncher.shutdown();
            }
            if (locationManagerLauncher != null) {
                locationManagerLauncher.shutdown();
            }
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            LOGGER.info("App tests finished!");
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    /**
     * Method is for unit tests where one has to make sure that all actions are removed from the action stack in order to minimize influence of other tests.
     *
     * @throws InterruptedException is thrown if the thread was externally interrupted
     */
    @AfterEach
    @Timeout(30)
    public void cancelAllOngoingActions() throws InterruptedException {
        LOGGER.info("Cancel all ongoing actions...");
        try {
            for (UnitController<?, ?> deviceController : deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().getEntries()) {
                deviceController.cancelAllActions();
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not cancel all ongoing actions!", ex, LOGGER);
        }
    }
}
