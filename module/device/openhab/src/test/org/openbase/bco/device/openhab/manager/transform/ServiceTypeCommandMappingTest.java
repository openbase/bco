package org.openbase.bco.device.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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


import org.openhab.core.types.Command;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;

import java.util.Set;

import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTypeCommandMappingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTypeCommandMappingTest.class);

    //    @BeforeClass
//    public static void setUpClass() throws InstantiationException {
//        MockRegistryHolder.newMockRegistry();
//    }
//
//    @AfterClass
//    public static void tearDownClass() {
//        MockRegistryHolder.shutdownMockRegistry();
//    }
//

    /**
     * Test if a transformer exists for every service type to command class mapping.
     * TODO: reactivate if mock registries contains needed meta config entries or a local registry is loaded for tests
     *
     * @throws CouldNotPerformException if the template registry is not available
     */
//    @Test
    public void testTransformerAvailability() throws CouldNotPerformException {
        LOGGER.info("testTransformerAvailability");

        boolean atLeastOneNotAvailable = false;
        for (ServiceTemplate serviceTemplate : Registries.getTemplateRegistry().getServiceTemplates()) {
            try {
                Set<Class<Command>> commandClasses = ServiceTypeCommandMapping.getCommandClasses(serviceTemplate.getServiceType());
                for (Class<Command> commandClass : commandClasses) {
                    try {
                        ServiceStateCommandTransformerPool.getInstance().getTransformer(serviceTemplate.getServiceType(), commandClass);
                    } catch (CouldNotPerformException ex) {
                        atLeastOneNotAvailable = true;
                        LOGGER.warn("Transformer for service type[" + serviceTemplate.getServiceType().name() + "] and command type["
                                + commandClass.getSimpleName() + "] it not available", ex);
                    }
                }
            } catch (NotAvailableException ex) {
                // ignore because no command classes for the service template entered
            }
        }
        assertFalse(atLeastOneNotAvailable);
    }
}
