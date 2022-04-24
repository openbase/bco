package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.app.cloudconnector.mapping.lib.Trait;
import org.openbase.bco.app.cloudconnector.mapping.unit.UnitTypeMapping;
import org.openbase.bco.authentication.mock.MqttIntegrationTest;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceStateTraitMapperFactoryTest extends MqttIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceStateTraitMapperFactoryTest.class);

    @BeforeEach
    public void setUp() throws Exception {
        MockRegistryHolder.newMockRegistry();

        Registries.getTemplateRegistry(true);
    }

    @AfterEach
    public void tearDown() {
        MockRegistryHolder.shutdownMockRegistry();
    }

    /**
     * Test if for all defined combinations of services and traits a mapper is available.
     */
    @Test
    @Timeout(value = 30)
    public void testMapperAvailability() {
        LOGGER.info("testMapperAvailability");

        final Map<String, CouldNotPerformException> mapperExceptionMap = new HashMap<>();
        final ServiceStateTraitMapperFactory serviceStateTraitMapperFactory = ServiceStateTraitMapperFactory.getInstance();

        for (final UnitTypeMapping unitTypeMapping : UnitTypeMapping.values()) {
            for (final Trait trait : unitTypeMapping.getTraitSet()) {
                final ServiceType serviceType = unitTypeMapping.getServiceType(trait);

                try {
                    serviceStateTraitMapperFactory.getServiceStateMapper(serviceType, trait);
                } catch (CouldNotPerformException ex) {
                    mapperExceptionMap.put(serviceType.name() + "_" + trait.name(), ex);
                }
            }
        }

        for (final Entry<String, CouldNotPerformException> entry : mapperExceptionMap.entrySet()) {
            LOGGER.error(entry.getValue().getMessage());
        }

        assertEquals("Could not create every needed mapping", 0, mapperExceptionMap.size());
    }

}
