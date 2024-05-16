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
import org.openbase.bco.dal.remote.layer.unit.SmokeDetectorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.AlarmStateType.AlarmState;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeDetectorRemoteTest extends AbstractBCODeviceManagerTest {

    private static SmokeDetectorRemote smokeDetectorRemote;

    @BeforeAll
    @Timeout(30)
    public static void setupTest() throws Throwable {
        smokeDetectorRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.SMOKE_DETECTOR), true, SmokeDetectorRemote.class);
    }

    /**
     * Test of getSmokeAlarmState method, of class SmokeDetectorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetSmokeAlarmState() throws Exception {
        System.out.println("getSmokeAlarmState");
        AlarmState alarmState = AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(smokeDetectorRemote.getId()).applyServiceState(alarmState, ServiceType.SMOKE_ALARM_STATE_SERVICE);
        smokeDetectorRemote.requestData().get();
        assertEquals(alarmState.getValue(), smokeDetectorRemote.getSmokeAlarmState().getValue(), "The getter for the smoke alarm state returns the wrong value!");
    }

    /**
     * Test of getSmokeState method, of class SmokeDetectorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetSmokeState() throws Exception {
        System.out.println("getSmokeState");
        SmokeState smokeState = SmokeState.newBuilder().setValue(SmokeState.State.SOME_SMOKE).setSmokeLevel(0.13d).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(smokeDetectorRemote.getId()).applyServiceState(smokeState, ServiceType.SMOKE_STATE_SERVICE);
        smokeDetectorRemote.requestData().get();
        assertEquals(smokeState.getValue(), smokeDetectorRemote.getSmokeState().getValue(), "The getter for the smoke state returns the wrong value!");
    }

}
