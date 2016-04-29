/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.test.remote.location;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.manager.location.remote.LocationRemote;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jps.exception.JPServiceException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author thuxohl
 */
public class LocationRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LocationRemoteTest.class);

    public LocationRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Used to test the activation of a scene via a button
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testLocation() throws Exception {
        LocationRegistryRemote locationRegistryRemote = new LocationRegistryRemote();
        locationRegistryRemote.init();
        locationRegistryRemote.activate();

        LocationRemote locationRemote = new LocationRemote();
        locationRemote.init(locationRegistryRemote.getLocationConfigsByLabel("Bath").get(0));
        locationRemote.activate();

        locationRemote.setPower(PowerState.newBuilder().setValue(PowerState.State.ON).build());
        locationRemote.setColor(HSVColor.newBuilder().setHue(0).setSaturation(100).setValue(100).build());
//        Double brighntess = 40d;
//        locationRemote.setBrightness(brighntess);
//
//        Thread.sleep(500);
//        assertEquals(brighntess, locationRemote.getBrightness());
    }
}
