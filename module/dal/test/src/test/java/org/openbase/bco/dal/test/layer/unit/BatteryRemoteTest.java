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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.remote.layer.unit.BatteryRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BatteryRemoteTest extends AbstractBCODeviceManagerTest {

    private static BatteryRemote batteryRemote;

    public BatteryRemoteTest() {
    }

    @BeforeAll
    public static void loadUnits() throws Throwable {
        batteryRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.BATTERY), true, BatteryRemote.class);
    }

    /**
     * Test of notifyUpdated method, of class BatteryRemote.
     */
    @Disabled
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBatteryLevel method, of class BatteryRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetBatteryLevel() throws Exception {
        try {
            System.out.println("getBatteryLevel");
            double level = 0.34d;
            BatteryState state = BatteryState.newBuilder().setLevel(level).build();
            deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(batteryRemote.getId()).applyServiceState(state, ServiceType.BATTERY_STATE_SERVICE);
            batteryRemote.requestData().get();
            assertEquals(state.getLevel(), batteryRemote.getBatteryState().getLevel(), 0.001, "The getter for the battery level returns the wrong value!");
            assertEquals(BatteryState.State.OK, batteryRemote.getData().getBatteryState().getValue(), "The battery state has not been updated according to the level!");

            BatteryState lastState = batteryRemote.getBatteryState();
            level = 0.095d;
            state = BatteryState.newBuilder().setLevel(level).setValue(State.CRITICAL).build();
            deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(batteryRemote.getId()).applyServiceState(state, ServiceType.BATTERY_STATE_SERVICE);
            batteryRemote.requestData().get();
            assertEquals(state.getLevel(), batteryRemote.getBatteryState().getLevel(), 0.001, "The getter for the battery level returns the wrong value!");
            assertEquals(state.getValue(), batteryRemote.getData().getBatteryState().getValue(), "The battery state value has not been updated correctly!");
            assertEquals(lastState, batteryRemote.getData().getBatteryStateLast(), "The last battery state has not been updated correctly!");
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LoggerFactory.getLogger(BatteryRemoteTest.class));
        }
    }
}
