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
import org.openbase.bco.dal.lib.layer.unit.RollerShutterController;
import org.openbase.bco.dal.remote.unit.RollerShutterRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import rst.domotic.state.BlindStateType.BlindState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RollerShutterRemoteTest extends AbstractBCODeviceManagerTest {

    private static RollerShutterRemote rollerShutterRemote;

    public RollerShutterRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        rollerShutterRemote = Units.getUnitsByLabel(MockRegistry.ROLLER_SHUTTER_LABEL, true, RollerShutterRemote.class).get(0);
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
        BlindState state = BlindState.newBuilder().setMovementState(BlindState.MovementState.DOWN).build();
        rollerShutterRemote.setBlindState(state).get();
        rollerShutterRemote.requestData().get();
        assertEquals("Shutter movement state has not been set in time!", state.getMovementState(), rollerShutterRemote.getData().getBlindState().getMovementState());
    }

    /**
     * Test of getShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetShutterState() throws Exception {
        System.out.println("getShutterState");
        BlindState state = BlindState.newBuilder().setMovementState(BlindState.MovementState.UP).build();
        ((RollerShutterController) deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(rollerShutterRemote.getId())).applyDataUpdate(state);
        rollerShutterRemote.requestData().get();
        assertEquals("Shutter has not been set in time!", rollerShutterRemote.getBlindState().getMovementState(), state.getMovementState());
    }

    /**
     * Test of notifyUpdated method, of class RollershutterRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }
}
