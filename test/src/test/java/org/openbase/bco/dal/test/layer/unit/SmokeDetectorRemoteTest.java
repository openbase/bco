package org.openbase.bco.dal.test.layer.unit;

/*
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.junit.*;
import org.openbase.bco.dal.remote.layer.unit.SmokeDetectorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.AlarmStateType.AlarmState;
import org.openbase.type.domotic.state.SmokeStateType.SmokeState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeDetectorRemoteTest extends AbstractBCODeviceManagerTest {

    private static SmokeDetectorRemote smokeDetectorRemote;

    public SmokeDetectorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        smokeDetectorRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.SMOKE_DETECTOR), true, SmokeDetectorRemote.class);
    }

    /**
     * Test of notifyUpdated method, of class SmokeDetectorRemote.
     *
     * @throws java.lang.Exception
     */
    @Ignore
    public void testNotifyUpdated() throws Exception {
    }

    /**
     * Test of getSmokeAlarmState method, of class SmokeDetectorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetSmokeAlarmState() throws Exception {
        System.out.println("getSmokeAlarmState");
        AlarmState alarmState = AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(smokeDetectorRemote.getId()).applyServiceState(alarmState, ServiceType.SMOKE_ALARM_STATE_SERVICE);
        smokeDetectorRemote.requestData().get();
        Assert.assertEquals("The getter for the smoke alarm state returns the wrong value!", alarmState.getValue(), smokeDetectorRemote.getSmokeAlarmState().getValue());
    }

    /**
     * Test of getSmokeState method, of class SmokeDetectorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetSmokeState() throws Exception {
        System.out.println("getSmokeState");
        SmokeState smokeState = SmokeState.newBuilder().setValue(SmokeState.State.SOME_SMOKE).setSmokeLevel(0.13d).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(smokeDetectorRemote.getId()).applyServiceState(smokeState, ServiceType.SMOKE_STATE_SERVICE);
        smokeDetectorRemote.requestData().get();
        Assert.assertEquals("The getter for the smoke state returns the wrong value!", smokeState.getValue(), smokeDetectorRemote.getSmokeState().getValue());
    }

}
