package org.openbase.bco.dal.remote.layer.unit;

/*-
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

import static org.junit.jupiter.api.Assertions.*;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;
import java.util.concurrent.TimeUnit;



public class CustomUnitPoolTest extends AbstractBCODeviceManagerTest {

    /**
     * Test of getBatteryLevel method, of class BatteryRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testUnitPool() throws Exception {
        final CustomUnitPool customUnitPool = new CustomUnitPool();
        assertEquals(false, customUnitPool.isActive(), "pool is active while never activated");

        customUnitPool.activate();

        customUnitPool.init(
                unitConfig -> unitConfig.hasId(),
                unitConfig -> unitConfig.getUnitType() == UnitType.BUTTON);

        customUnitPool.activate();

        for (UnitRemote<? extends Message> unitRemote : customUnitPool.getInternalUnitList()) {
            assertEquals(UnitType.BUTTON, unitRemote.getUnitType(), "pool contains actually filtered entry!");
            System.out.println("is button: "+ unitRemote.getLabel());
        }

        final List<UnitConfig> buttonUnitConfig = Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.BUTTON);
        Registries.getUnitRegistry().updateUnitConfig(buttonUnitConfig.get(0).toBuilder().addAlias("MyButtonTestUnit").build()).get(5, TimeUnit.SECONDS);

        final List<UnitConfig> lightUnitConfig = Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.COLORABLE_LIGHT);
        Registries.getUnitRegistry().updateUnitConfig(lightUnitConfig.get(0).toBuilder().addAlias("MyLightestUnit").build()).get(5, TimeUnit.SECONDS);

        for (UnitRemote<? extends Message> unitRemote : customUnitPool.getInternalUnitList()) {
            assertEquals(UnitType.BUTTON, unitRemote.getUnitType(), "pool contains actually filtered entry!");
            System.out.println("is button: "+ unitRemote.getLabel());
        }

        customUnitPool.shutdown();
    }
}
