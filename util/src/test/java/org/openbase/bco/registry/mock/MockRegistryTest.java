package org.openbase.bco.registry.mock;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.Stopwatch;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MockRegistryTest {

    public MockRegistryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws JPServiceException {
        JPService.setupJUnitTestMode();
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of MockRegistry creation.
     *
     * @throws org.openbase.jul.exception.InstantiationException
     */
    @Test(timeout = 60000)
    public void testMockRegistryCreation() throws Exception {
        Stopwatch stopwatch = new Stopwatch();
        List<Long> times = new ArrayList<>();
        System.out.println("testMockRegistryCreation");
        try {
            for (int i = 0; i < 10; ++i) {
                System.out.println("start mock registry");
                stopwatch.restart();
                MockRegistryHolder.newMockRegistry();
                System.out.println("shutdown mock registry");
                MockRegistryHolder.shutdownMockRegistry();
                times.add(stopwatch.stop());
            }
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, System.err);
        }
        for (Long time : times) {
            System.out.println("Startup + Shutdown took: " + time + "ms");
        }
    }
}
