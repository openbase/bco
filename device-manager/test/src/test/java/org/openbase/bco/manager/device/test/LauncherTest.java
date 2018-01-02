package org.openbase.bco.manager.device.test;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.junit.Test;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LauncherTest extends AbstractBCOTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LauncherTest.class);

    public LauncherTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCOTest.setUpClass();
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        AbstractBCOTest.tearDownClass();
    }

    @Before
    public void setUp() throws InitializationException, org.openbase.jul.exception.InstantiationException {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of deactivate method, of class DeviceManagerLauncher.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testShutdown() throws Exception {
        DeviceManagerLauncher instance = new DeviceManagerLauncher();
        try {
            instance.launch();
        } catch (CouldNotPerformException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
        instance.shutdown();
    }
}
