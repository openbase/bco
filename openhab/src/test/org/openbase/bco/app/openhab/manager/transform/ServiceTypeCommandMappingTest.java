package org.openbase.bco.app.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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


import org.eclipse.smarthome.core.types.Command;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTypeCommandMappingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTypeCommandMappingTest.class);

    @BeforeClass
    public static void setUpClass() throws InstantiationException {
        MockRegistryHolder.newMockRegistry();
    }

    @AfterClass
    public static void tearDownClass() {
        MockRegistryHolder.shutdownMockRegistry();
    }

    @Test
    public void testFromServiceType() throws NotAvailableException {
        LOGGER.info("testFromServiceType");

        for (ServiceTypeCommandMapping value : ServiceTypeCommandMapping.values()) {
            assertEquals(value, ServiceTypeCommandMapping.fromServiceType(value.getServiceType()));
        }
    }

    @Test
    public void testTransformerAvailability() {
        LOGGER.info("testTransformerAvailability");

        boolean atLeastOneNotAvailable = false;
        for (ServiceTypeCommandMapping value : ServiceTypeCommandMapping.values()) {
            for (Class<? extends Command> commandClass : value.getCommandClasses()) {
                try {
                    ServiceStateCommandTransformerPool.getInstance().getTransformer(value.getServiceType(), commandClass);
                } catch (CouldNotPerformException ex) {
                    atLeastOneNotAvailable = true;
                    LOGGER.warn("Transformer for service type[" + value.getServiceType().name() + "] and command type["
                            + commandClass.getSimpleName() + "] it not available", ex);
                }
            }
        }
        assertFalse(atLeastOneNotAvailable);
    }
}
