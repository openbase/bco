package org.openbase.bco.dal.test.layer.unit.device;

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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class AbstractBCODeviceManagerTest extends AbstractBCOTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBCODeviceManagerTest.class);

    protected static DeviceManagerLauncher deviceManagerLauncher;

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOTest.setUpClass();

            deviceManagerLauncher = new DeviceManagerLauncher();
            deviceManagerLauncher.launch().get();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (deviceManagerLauncher != null) {
                deviceManagerLauncher.shutdown();
            }
            AbstractBCOTest.tearDownClass();
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
    @After
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
