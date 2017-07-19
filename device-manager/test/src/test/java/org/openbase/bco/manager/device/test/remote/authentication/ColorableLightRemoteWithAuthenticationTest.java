package org.openbase.bco.manager.device.test.remote.authentication;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.core.mock.MockCredentialStore;
import org.openbase.bco.authentication.lib.CredentialStore;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.state.PowerStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class ColorableLightRemoteWithAuthenticationTest extends AbstractBCODeviceManagerTest {

    private static AuthenticatorController authenticatorController;
    private static CredentialStore credentialStore;

    private static ColorableLightRemote colorableLightRemote;

    public ColorableLightRemoteWithAuthenticationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        credentialStore = new MockCredentialStore();

        authenticatorController = new AuthenticatorController(credentialStore);
        authenticatorController.init();
        authenticatorController.activate();
        authenticatorController.waitForActivation();
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        AbstractBCODeviceManagerTest.tearDownClass();
        if (authenticatorController != null) {
            authenticatorController.shutdown();
        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test of setColor method, of class AmbientLightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColorWithAuthentication() throws Exception {
        System.out.println("login prior to requesting units");

        String clientId = MockCredentialStore.USER_ID;
        String password = MockCredentialStore.USER_PASSWORD;

        SessionManager manager = SessionManager.getInstance();
        boolean result = manager.login(clientId, password);
        assertEquals(true, result);

        colorableLightRemote = Units.getUnitsByLabel(MockRegistry.COLORABLE_LIGHT_LABEL, true, ColorableLightRemote.class).get(0);

        colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", PowerStateType.PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());

        // logout
        SessionManager.getInstance().logout();
    }
}
