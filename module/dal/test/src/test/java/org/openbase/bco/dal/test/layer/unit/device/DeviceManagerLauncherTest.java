package org.openbase.bco.dal.test.layer.unit.device;

/*
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
import org.junit.jupiter.api.*;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceManagerLauncherTest extends AbstractBCOTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceManagerLauncherTest.class);

    public DeviceManagerLauncherTest() {
    }
    
    @BeforeAll
    public static void setUpClass() throws Throwable {
        AbstractBCOTest.setUpClass();
    }

    @AfterAll
    public static void tearDownClass() throws Throwable {
        AbstractBCOTest.tearDownClass();
    }

    @BeforeEach
    public void setUp() throws InitializationException, org.openbase.jul.exception.InstantiationException {
    }

    /**
     * Test of deactivate method, of class DeviceManagerLauncher.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testShutdown() throws Exception {
        DeviceManagerLauncher instance = new DeviceManagerLauncher();
        try {
            instance.launch().get();
        } catch (ExecutionException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
        instance.shutdown();
    }
}
