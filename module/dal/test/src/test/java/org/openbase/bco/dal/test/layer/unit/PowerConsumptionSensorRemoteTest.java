package org.openbase.bco.dal.test.layer.unit;

/*
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.remote.layer.unit.PowerConsumptionSensorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerConsumptionSensorRemoteTest extends AbstractBCODeviceManagerTest {

    private static PowerConsumptionSensorRemote powerConsumptionRemote;

    public PowerConsumptionSensorRemoteTest() {
    }

    @BeforeAll
    @Timeout(30)
    public static void loadUnits() throws Throwable {
        powerConsumptionRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.POWER_CONSUMPTION_SENSOR), true, PowerConsumptionSensorRemote.class);
    }

    /**
     * Test of notifyUpdated method, of class PowerConsumptionSensorRemote.
     */
    @Disabled
    public void testNotifyUpdated() {
    }

    /**
     * Test of getPowerConsumption method, of class
     * PowerConsumptionSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetPowerConsumption() throws Exception {
        System.out.println("getPowerConsumption");
        double consumption = 200d;
        double voltage = 100d;
        double current = 2d;
        PowerConsumptionState state = PowerConsumptionState.newBuilder().setConsumption(consumption).setCurrent(current).setVoltage(voltage).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(powerConsumptionRemote.getId()).applyServiceState(state, ServiceType.POWER_CONSUMPTION_STATE_SERVICE);
        powerConsumptionRemote.requestData().get();
        assertEquals(state.getVoltage(), powerConsumptionRemote.getPowerConsumptionState().getVoltage(), 0.1, "The getter for the power consumption returns the wrong voltage value!");
        assertEquals(state.getConsumption(), powerConsumptionRemote.getPowerConsumptionState().getConsumption(), 0.1, "The getter for the power consumption returns the wrong consumption value!");
        assertEquals(state.getCurrent(), powerConsumptionRemote.getPowerConsumptionState().getCurrent(), 0.1, "The getter for the power consumption returns the wrong current value!");
    }
}
