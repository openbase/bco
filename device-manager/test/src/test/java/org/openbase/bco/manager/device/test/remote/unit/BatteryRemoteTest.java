package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.unit.BatteryController;
import org.openbase.bco.dal.remote.unit.BatteryRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;
import rst.domotic.state.BatteryStateType.BatteryState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BatteryRemoteTest extends AbstractBCODeviceManagerTest {
    
    private static BatteryRemote batteryRemote;
    
    public BatteryRemoteTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        
        batteryRemote = Units.getUnitsByLabel(MockRegistry.BATTERY_LABEL, true, BatteryRemote.class).get(0);
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class BatteryRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getBattaryLevel method, of class BatteryRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetBatteryLevel() throws Exception {
        try {
            System.out.println("getBatteryLevel");
            double level = 34.0;
            BatteryState state = BatteryState.newBuilder().setLevel(level).build();
            ((BatteryController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(batteryRemote.getId())).applyDataUpdate(state);
            batteryRemote.requestData().get();
            assertEquals("The getter for the battery level returns the wrong value!", state.getLevel(), batteryRemote.getBatteryState().getLevel(), 0.1);
            assertEquals("The battery state has not been updated according to the level!", BatteryState.State.OK, batteryRemote.getData().getBatteryState().getValue());
            
            BatteryState lastState = batteryRemote.getBatteryState();
            level = 9.5;
            state = BatteryState.newBuilder().setLevel(level).setValue(BatteryState.State.INSUFFICIENT).build();
            ((BatteryController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(batteryRemote.getId())).applyDataUpdate(state);
            batteryRemote.requestData().get();
            assertEquals("The getter for the battery level returns the wrong value!", state.getLevel(), batteryRemote.getBatteryState().getLevel(), 0.1);
            assertEquals("The battery state value has not been updated correctly!", state.getValue(), batteryRemote.getData().getBatteryState().getValue());
            assertEquals("The last battery state has not been updated correctly!", lastState, batteryRemote.getData().getBatteryStateLast());
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LoggerFactory.getLogger(BatteryRemoteTest.class));
        }
    }    
}
