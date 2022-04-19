package org.openbase.bco.app.cloudconnector.mapping.unit;

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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitTypeMappingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitTypeMapping.class);

    /**
     * Test if each unitTypeMapping matches its expected unitType.
     *
     * @throws IllegalArgumentException if the name of a unitTypeMapping does not lead to the according unitType.
     */
    @Test
    public void testUnitTypeValidity() throws IllegalArgumentException {
        LOGGER.info("testUnitTypeValidity");
        for (final UnitTypeMapping unitTypeMapping : UnitTypeMapping.values()) {
            final String unitTypeName = unitTypeMapping.name().replace(UnitTypeMapping.POSTFIX, "");

            final UnitType unitType = UnitType.valueOf(unitTypeName);

            assertEquals("UnitTypeMapping[" + unitTypeMapping.name() + "] does not match unitType[" + unitType.name() + "]",
                    unitTypeMapping.getUnitType(), unitType);
        }
    }
}
