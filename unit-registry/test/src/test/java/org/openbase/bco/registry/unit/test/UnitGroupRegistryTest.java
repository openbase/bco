package org.openbase.bco.registry.unit.test;

/*-
 * #%L
 * BCO Registry Unit Test
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

import org.junit.Test;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class UnitGroupRegistryTest extends AbstractBCORegistryTest {

    /**
     * Test if changing the placement of a unit group works.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void testPlacementChange() throws Exception {
        logger.info("testPlacementChange");

        UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, "PlacementChangeGroup");
        unitConfig.setUnitType(UnitType.UNIT_GROUP);

        PlacementConfig.Builder placement = unitConfig.getPlacementConfigBuilder();
        placement.setLocationId(Registries.getUnitRegistry().getRootLocationConfig().getId());

        UnitGroupConfig.Builder unitGroupConfig = unitConfig.getUnitGroupConfigBuilder();
        unitGroupConfig.setUnitType(UnitType.COLORABLE_LIGHT);
        unitGroupConfig.addMemberId(Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.COLORABLE_LIGHT).get(0).getId());

        UnitConfig registeredGroup = Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get();

        assertEquals("BoundingBox was not generated!", registeredGroup.getPlacementConfig().getShape().getBoundingBox(), MockRegistry.DEFAULT_BOUNDING_BOX);
    }
}
