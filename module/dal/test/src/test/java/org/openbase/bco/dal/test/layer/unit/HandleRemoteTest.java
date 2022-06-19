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
import org.openbase.bco.dal.remote.layer.unit.HandleRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.HandleStateType.HandleState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class HandleRemoteTest extends AbstractBCODeviceManagerTest {

    private static HandleRemote handleRemote;

    public HandleRemoteTest() {
    }

    @BeforeAll
    public static void loadUnits() throws Throwable {
        handleRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.HANDLE), true, HandleRemote.class);
    }

    /**
     * Test of notifyUpdated method, of class HandleSensorRemote.
     */
    @Disabled
    public void testNotifyUpdated() {
    }

    /**
     * Test of getRotaryHandleState method, of class HandleSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetRotaryHandleState() throws Exception {
        System.out.println("getRotaryHandleState");
        HandleState handlestate =HandleState.newBuilder().setPosition(90).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(handleRemote.getId()).applyServiceState(handlestate, ServiceType.HANDLE_STATE_SERVICE);
        handleRemote.requestData().get();
        assertEquals(handlestate.getPosition(), handleRemote.getHandleState().getPosition(), "The getter for the handle state returns the wrong value!");
    }
}
