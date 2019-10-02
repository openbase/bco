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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.control.layer.unit.agent.AgentManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.app.AppManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observer;
import org.openbase.type.domotic.authentication.UserClientPairType;
import org.slf4j.LoggerFactory;

public class BCOAppTest extends AbstractBCOTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractBCOAgentManagerTest.class);

    protected static AgentManagerLauncher agentManagerLauncher;
    protected static AppManagerLauncher appManagerLauncher;
    protected static DeviceManagerLauncher deviceManagerLauncher;
    protected static LocationManagerLauncher locationManagerLauncher;

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            LOGGER.info("Start app test setup...");
            AbstractBCOTest.setUpClass();

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

            LOGGER.trace("Finally wait for registry...");
            Registries.waitForData();
            LOGGER.info("Ready to test...");

            BCOLogin.getSession().loginBCOUser();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        LOGGER.info("Tear down app tests...");
        try {
            if(appManagerLauncher != null) {
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
            AbstractBCOTest.tearDownClass();
            LOGGER.info("App tests finished!");
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }
}
