package org.openbase.bco.dal.remote.layer.unit

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.openbase.bco.dal.lib.layer.unit.UnitRemote
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.pattern.Filter
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitTemplateType
import java.util.concurrent.TimeUnit

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
class CustomUnitPoolTest : AbstractBCODeviceManagerTest() {
    /**
     * Test of getBatteryLevel method, of class BatteryRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    @Throws(Exception::class)
    fun testUnitPool() {
        val customUnitPool: CustomUnitPool<*, *> = CustomUnitPool<UnitConfig, UnitRemote<UnitConfig>>()
        Assertions.assertEquals(false, customUnitPool.isActive(), "pool is active while never activated")
        customUnitPool.activate()
        customUnitPool.init(
            Filter<UnitConfig> { unitConfig: UnitConfig -> unitConfig.hasId() },
            Filter<UnitConfig> { unitConfig: UnitConfig -> unitConfig.unitType === UnitTemplateType.UnitTemplate.UnitType.BUTTON })
        customUnitPool.activate()
        for (unitRemote in customUnitPool.internalUnitList) {
            Assertions.assertEquals(
                UnitTemplateType.UnitTemplate.UnitType.BUTTON,
                unitRemote.getUnitType(),
                "pool contains actually filtered entry!"
            )
            println("is button: " + unitRemote.getLabel())
        }
        val buttonUnitConfig =
            Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitTemplateType.UnitTemplate.UnitType.BUTTON)
        Registries.getUnitRegistry()
            .updateUnitConfig(buttonUnitConfig[0].toBuilder().addAlias("MyButtonTestUnit").build())[5, TimeUnit.SECONDS]
        val lightUnitConfig = Registries.getUnitRegistry()
            .getUnitConfigsByUnitType(UnitTemplateType.UnitTemplate.UnitType.COLORABLE_LIGHT)
        Registries.getUnitRegistry()
            .updateUnitConfig(lightUnitConfig[0].toBuilder().addAlias("MyLightestUnit").build())[5, TimeUnit.SECONDS]
        for (unitRemote in customUnitPool.internalUnitList) {
            Assertions.assertEquals(
                UnitTemplateType.UnitTemplate.UnitType.BUTTON,
                unitRemote.getUnitType(),
                "pool contains actually filtered entry!"
            )
            println("is button: " + unitRemote.getLabel())
        }
        customUnitPool.shutdown()
    }
}
