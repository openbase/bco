package org.openbase.bco.registry.mock;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.openbase.bco.authentication.mock.MqttIntegrationTest;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.Stopwatch;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MockRegistryTest extends MqttIntegrationTest {

    public MockRegistryTest() {
    }

    @BeforeAll
    public static void setUpClass() throws JPServiceException {
        JPService.setupJUnitTestMode();
    }

    /**
     * Test of MockRegistry creation.
     *
     * @throws org.openbase.jul.exception.InstantiationException
     */
    @Test
    @Timeout(120)
    public void testMockRegistryCreation() throws Exception {
        Stopwatch stopwatch = new Stopwatch();
        List<String> times = new ArrayList<>();
        System.out.println("testMockRegistryCreation");
        try {
            for (int i = 0; i < 5; ++i) {
                System.out.println("start mock registry");
                stopwatch.restart();
                MockRegistryHolder.newMockRegistry();
                times.add("Startup ["+(i + 1)+"] took: " + stopwatch.getTime() + "ms");
                stopwatch.restart();
                System.out.println("shutdown mock registry");
                MockRegistryHolder.shutdownMockRegistry();
                times.add("Shutdown ["+(i + 1)+"] took: " + stopwatch.stop() + "ms");
            }
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, System.err);
        }

        System.out.println("Summarized results:");
        for (String time : times) {
            System.out.println(time);
        }
    }
}
