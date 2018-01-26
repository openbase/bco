package org.openbase.bco.manager.device.test.remote.authentication;

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
import org.junit.Test;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rst.processing.TimestampJavaTimeTransform;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketType.Ticket;
import rst.domotic.state.PowerStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.timing.IntervalType;
import rst.timing.IntervalType.Interval;

/**
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class ColorableLightRemoteWithAuthenticationTest extends AbstractBCODeviceManagerTest {

    private static ColorableLightRemote colorableLightRemote;
    
    private final SessionManager sessionManager;
    
    public ColorableLightRemoteWithAuthenticationTest() {
        sessionManager = new SessionManager();
    }

    @Before
    public void setUp() throws CouldNotPerformException, InterruptedException {
        String adminId = MockRegistry.admin.getId();
        String password = UserCreationPlugin.DEFAULT_ADMIN_USERNAME_AND_PASSWORD;

        boolean result = sessionManager.login(adminId, password);
        assertEquals(true, result);
        
        colorableLightRemote = Units.getUnitsByLabel(MockRegistry.COLORABLE_LIGHT_LABEL, true, ColorableLightRemote.class).get(0);
        colorableLightRemote.setSessionManager(sessionManager);
    }

    @After
    public void tearDown() throws CouldNotPerformException {
        sessionManager.logout();
        colorableLightRemote.setSessionManager(SessionManager.getInstance());
    }

    /**
     * Test if executing an action works with prior user login and authentication.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetColorWithAuthentication() throws Exception {
        System.out.println("testSetColorWithAuthentication");

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

        UnitConfig.Builder testClient = UnitConfig.newBuilder();
        testClient.setType(UnitType.USER);
        UserConfig.Builder userConfig = testClient.getUserConfigBuilder();
        userConfig.setUserName("UnitTestClient");
        userConfig.setFirstName("First");
        userConfig.setLastName("Last");
        testClient.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        UnitConfig registered = Registries.getUserRegistry().registerUserConfig(testClient.build()).get();
        
        // register client
        System.out.println("register client");
        sessionManager.registerClient(registered.getId());
        
        // logout admin
        System.out.println("logout admin");
        sessionManager.logout();
        
        // login client
        System.out.println("login client");
        boolean result = sessionManager.login(registered.getId());
        assertEquals(true, result);
        
        // make ticket invalid
        byte[] serviceServerSecretKey = AuthenticatedServerManager.getInstance().getServiceServerSecretKey();
        System.out.println("make ticket invalid");
        Ticket ticket = EncryptionHelper.decryptSymmetric(sessionManager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);
        
        long currentTime = 0;
        Interval.Builder validityInterval = IntervalType.Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + JPService.getProperty(JPSessionTimeout.class).getValue()));
        validityInterval.build();
        
        Ticket.Builder cstb = ticket.toBuilder();
        cstb.setValidityPeriod(validityInterval.build());

        TicketAuthenticatorWrapper.Builder wrapperBuilder = sessionManager.getTicketAuthenticatorWrapper().toBuilder();
        wrapperBuilder.setTicket(EncryptionHelper.encryptSymmetric(cstb.build(), serviceServerSecretKey));
          
        sessionManager.setTicketAuthenticatorWrapper(wrapperBuilder.build());
        
        // execute action
        System.out.println("execute action");
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
                        
        byte[] serviceServerSecretKey = AuthenticatedServerManager.getInstance().getServiceServerSecretKey();
        // make ticket invalid
        System.out.println("make ticket invalid");
        Ticket ticket = EncryptionHelper.decryptSymmetric(sessionManager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);
        
        long currentTime = 0;
        Interval.Builder validityInterval = IntervalType.Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + JPService.getProperty(JPSessionTimeout.class).getValue()));
        validityInterval.build();
        
        Ticket.Builder cstb = ticket.toBuilder();
        cstb.setValidityPeriod(validityInterval.build());

        TicketAuthenticatorWrapper.Builder wrapperBuilder = sessionManager.getTicketAuthenticatorWrapper().toBuilder();
        wrapperBuilder.setTicket(EncryptionHelper.encryptSymmetric(cstb.build(), serviceServerSecretKey));
          
        sessionManager.setTicketAuthenticatorWrapper(wrapperBuilder.build());
        
        // execute action
        System.out.println("execute action");
        colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON).get();
        colorableLightRemote.requestData().get();
        assertEquals("Power has not been set in time!", PowerStateType.PowerState.State.ON, colorableLightRemote.getData().getPowerState().getValue());
    }
}
