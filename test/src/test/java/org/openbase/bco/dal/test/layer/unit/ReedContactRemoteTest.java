package org.openbase.bco.dal.test.layer.unit;

/*
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.bco.dal.remote.layer.unit.ReedContactRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ContactStateType.ContactState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ReedContactRemoteTest extends AbstractBCODeviceManagerTest {

    private static ReedContactRemote reedContactRemote;

    public ReedContactRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        reedContactRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.REED_CONTACT), true, ReedContactRemote.class);
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class ReedSwitchRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getReedSwitchState method, of class ReedSwitchRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetReedSwitchState() throws Exception {
        System.out.println("getReedSwitchState");
        ContactState state = ContactState.newBuilder().setValue(ContactState.State.OPEN).build();
        deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(reedContactRemote.getId()).applyServiceState(state, ServiceType.CONTACT_STATE_SERVICE);
        reedContactRemote.requestData().get();
        Assert.assertEquals("The getter for the reed switch state returns the wrong value!", state.getValue(), reedContactRemote.getContactState().getValue());
    }
}
