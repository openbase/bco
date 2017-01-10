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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;

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
    public void testMockRegistryCreation() throws InstantiationException, InterruptedException, CouldNotPerformException {
        System.out.println("testMockRegistryCreation");
        try {
            for (int i = 0; i < 1; ++i) {
                System.out.println("start mock registry");
                MockRegistryHolder.newMockRegistry();
                System.out.println("shutdown mock registry");
                MockRegistryHolder.shutdownMockRegistry();
            }
        } catch (InstantiationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, System.err);
        }
    }
}
