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
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.bco.dal.remote.layer.unit.LightSensorRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LightSensorRemoteTest extends AbstractBCODeviceManagerTest {

    private static LightSensorRemote lightSensorRemote;

    public LightSensorRemoteTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        lightSensorRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.LIGHT_SENSOR), true, LightSensorRemote.class);
    }

    /**
     * Test of getIlluminance method, of class LightSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetIlluminance() throws Exception {
        System.out.println("getIlluminance");
        double illuminance = 0.5;
        IlluminanceState illuminanceState = IlluminanceState.newBuilder().setIlluminance(illuminance).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(lightSensorRemote.getId()).applyServiceState(illuminanceState, ServiceType.ILLUMINANCE_STATE_SERVICE);
        lightSensorRemote.requestData().get();
        assertEquals("The getter for the illuminance returns the wrong value!", illuminanceState.getIlluminance(), lightSensorRemote.getIlluminanceState().getIlluminance(), 0.1);
    }
}
