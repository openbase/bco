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
import org.openbase.bco.authentication.lib.mock.MockClientStore;
import org.openbase.bco.authentication.core.mock.MockCredentialStore;
import static org.openbase.bco.authentication.lib.AuthenticationServerHandler.VALIDITY_PERIOD_IN_MILLIS;
import org.openbase.bco.authentication.lib.CredentialStore;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.rst.processing.TimestampJavaTimeTransform;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketType.Ticket;
import rst.domotic.state.PowerStateType;
import rst.timing.IntervalType;
import rst.timing.IntervalType.Interval;

/**
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class ColorableLightRemoteWithAuthenticationTest extends AbstractBCODeviceManagerTest {

    private static AuthenticatorController authenticatorController;
    private static CredentialStore credentialStore;

    private static ColorableLightRemote colorableLightRemote;
    
    private static byte[] serviceServerSecretKey;

    public ColorableLightRemoteWithAuthenticationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();
        
        serviceServerSecretKey = EncryptionHelper.generateKey();

        credentialStore = new MockCredentialStore();

        authenticatorController = new AuthenticatorController(credentialStore, serviceServerSecretKey);
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
        SessionManager.getInstance().logout();
    }

    /**
     * Test if executing an action works with prior user login and authentication.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColorWithAuthentication() throws Exception {
        System.out.println("testSetColorWithAuthentication");

        String clientId = MockCredentialStore.USER_ID;
        String password = MockCredentialStore.USER_PASSWORD;

        SessionManager manager = SessionManager.getInstance();
        boolean result = manager.login(clientId, password);
        assertEquals(true, result);

        colorableLightRemote = Units.getUnitsByLabel(MockRegistry.COLORABLE_LIGHT_LABEL, true, ColorableLightRemote.class).get(0);

        colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", PowerStateType.PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test if executing an action works with prior client login and authentication, but then with an expired session.
     * So first, the relog should be executed for a client and then the action should be executed.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColorWithClientWithoutAuthentication() throws Exception {
        System.out.println("testSetColorWithClientWithoutAuthentication");

        String clientId = MockClientStore.ADMIN_ID;
        String password = MockClientStore.ADMIN_PASSWORD;

        // login admin
        System.out.println("login admin");
        SessionManager manager = SessionManager.getInstance();
        boolean result = manager.login(clientId, password);
        assertEquals(true, result);
        
        // register client
        System.out.println("register client");
        manager.registerClient(MockClientStore.CLIENT_ID);
        
        // logout admin
        System.out.println("logout admin");
        manager.logout();
        
        // login client
        System.out.println("login client");
        result = manager.login(MockClientStore.CLIENT_ID);
        assertEquals(true, result);
        
        // make ticket invalid
        System.out.println("make ticket invalid");
        Ticket ticket = EncryptionHelper.decryptSymmetric(manager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);
        
        long currentTime = 0;
        Interval.Builder validityInterval = IntervalType.Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + VALIDITY_PERIOD_IN_MILLIS));
        validityInterval.build();
        
        Ticket.Builder cstb = ticket.toBuilder();
        cstb.setValidityPeriod(validityInterval.build());

        TicketAuthenticatorWrapper.Builder wrapperBuilder = manager.getTicketAuthenticatorWrapper().toBuilder();
        wrapperBuilder.setTicket(EncryptionHelper.encryptSymmetric(cstb.build(), serviceServerSecretKey));
          
        manager.setTicketAuthenticatorWrapper(wrapperBuilder.build());
        
        // execute action
        System.out.println("execute action");
        colorableLightRemote = Units.getUnitsByLabel(MockRegistry.COLORABLE_LIGHT_LABEL, true, ColorableLightRemote.class).get(0);

        colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", PowerStateType.PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test if executing an action works with prior user login and authentication, but then with an expired session.
     * So first, the relog should be executed for a user and then the action should be executed.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColorWithUserWithoutAuthentication() throws Exception {
        System.out.println("testSetColorWithClientWithoutAuthentication");

        String clientId = MockClientStore.ADMIN_ID;
        String password = MockClientStore.ADMIN_PASSWORD;

        // login admin
        System.out.println("login admin");
        SessionManager manager = SessionManager.getInstance();
        boolean result = manager.login(clientId, password, true);
        assertEquals(true, result);
                        
        // make ticket invalid
        System.out.println("make ticket invalid");
        Ticket ticket = EncryptionHelper.decryptSymmetric(manager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);
        
        long currentTime = 0;
        Interval.Builder validityInterval = IntervalType.Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + VALIDITY_PERIOD_IN_MILLIS));
        validityInterval.build();
        
        Ticket.Builder cstb = ticket.toBuilder();
        cstb.setValidityPeriod(validityInterval.build());

        TicketAuthenticatorWrapper.Builder wrapperBuilder = manager.getTicketAuthenticatorWrapper().toBuilder();
        wrapperBuilder.setTicket(EncryptionHelper.encryptSymmetric(cstb.build(), serviceServerSecretKey));
          
        manager.setTicketAuthenticatorWrapper(wrapperBuilder.build());
        
        // execute action
        System.out.println("execute action");
        colorableLightRemote = Units.getUnitsByLabel(MockRegistry.COLORABLE_LIGHT_LABEL, true, ColorableLightRemote.class).get(0);

        colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", PowerStateType.PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());
    }
}
