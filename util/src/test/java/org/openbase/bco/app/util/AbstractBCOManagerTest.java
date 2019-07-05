package org.openbase.bco.app.util;

/*-
 * #%L
 * BCO App Utility
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
import org.openbase.bco.dal.control.layer.unit.agent.AgentManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.app.AppManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.scene.SceneManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.user.UserManagerLauncher;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class AbstractBCOManagerTest extends AbstractBCOTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBCOManagerTest.class);

    protected static AgentManagerLauncher agentManagerLauncher;
    protected static AppManagerLauncher appManagerLauncher;
    protected static DeviceManagerLauncher deviceManagerLauncher;
    protected static LocationManagerLauncher locationManagerLauncher;
    protected static SceneManagerLauncher sceneManagerLauncher;
    protected static UserManagerLauncher userManagerLauncher;

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOTest.setUpClass();

            agentManagerLauncher = new AgentManagerLauncher();
            agentManagerLauncher.launch().get();
            appManagerLauncher = new AppManagerLauncher();
            appManagerLauncher.launch().get();
            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch().get();
            locationManagerLauncher = new LocationManagerLauncher();
            locationManagerLauncher.launch().get();
            sceneManagerLauncher = new SceneManagerLauncher();
            sceneManagerLauncher.launch().get();
            userManagerLauncher = new UserManagerLauncher();
            userManagerLauncher.launch().get();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (agentManagerLauncher != null) {
                agentManagerLauncher.shutdown();
            }
            if (appManagerLauncher != null) {
                appManagerLauncher.shutdown();
            }
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            if (locationManagerLauncher != null) {
                locationManagerLauncher.shutdown();
            }
            if (sceneManagerLauncher != null) {
                sceneManagerLauncher.shutdown();
            }
            if (userManagerLauncher != null) {
                userManagerLauncher.shutdown();
            }

            AbstractBCOTest.tearDownClass();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }
}
