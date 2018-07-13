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

import org.junit.*;
import org.openbase.bco.dal.remote.unit.RollerShutterRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RollerShutterRemoteTest extends AbstractBCODeviceManagerTest {

    private static RollerShutterRemote rollerShutterRemote;

    public RollerShutterRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        rollerShutterRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.ROLLER_SHUTTER), true, RollerShutterRemote.class);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetShutterState() throws Exception {
        System.out.println("setShutterState");
        BlindState state = BlindState.newBuilder().setValue(BlindState.State.DOWN).build();
        rollerShutterRemote.setBlindState(state).get();
        rollerShutterRemote.requestData().get();
        assertEquals("Shutter movement state has not been set in time!", state.getValue(), rollerShutterRemote.getData().getBlindState().getValue());
    }

    /**
     * Test of getShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetShutterState() throws Exception {
        System.out.println("getShutterState");
        BlindState state = TimestampProcessor.updateTimestampWithCurrentTime(BlindState.newBuilder().setValue(BlindState.State.UP)).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(rollerShutterRemote.getId()).applyDataUpdate(state, ServiceType.BLIND_STATE_SERVICE);
        rollerShutterRemote.requestData().get();
        assertEquals("Shutter has not been set in time!", rollerShutterRemote.getBlindState().getValue(), state.getValue());
    }

    /**
     * Test of notifyUpdated method, of class RollershutterRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
