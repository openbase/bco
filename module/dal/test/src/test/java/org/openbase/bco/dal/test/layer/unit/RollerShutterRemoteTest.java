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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.remote.layer.unit.RollerShutterRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.BlindStateType.BlindState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RollerShutterRemoteTest extends AbstractBCODeviceManagerTest {

    private static RollerShutterRemote rollerShutterRemote;

    public RollerShutterRemoteTest() {
    }

    @BeforeAll
    @Timeout(30)
    public static void loadUnits() throws Throwable {
        rollerShutterRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.ROLLER_SHUTTER), true, RollerShutterRemote.class);
    }

    /**
     * Test of setShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetShutterState() throws Exception {
        System.out.println("setShutterState");
        BlindState state = BlindState.newBuilder().setValue(BlindState.State.DOWN).build();
        waitForExecution(rollerShutterRemote.setBlindState(state));
        assertEquals(state.getValue(), rollerShutterRemote.getData().getBlindState().getValue(), "Shutter movement state has not been set in time!");
    }

    /**
     * Test of getShutterState method, of class RollershutterRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetShutterState() throws Exception {
        System.out.println("getShutterState");
        final BlindState blindState = BlindState.newBuilder().setValue(State.UP).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(rollerShutterRemote.getId()).applyServiceState(blindState, ServiceType.BLIND_STATE_SERVICE);
        rollerShutterRemote.requestData().get();
        assertEquals(rollerShutterRemote.getBlindState().getValue(), blindState.getValue(), "Shutter has not been set in time!");
    }
}
