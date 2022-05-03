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
import org.junit.jupiter.api.*;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.session.TokenGenerator;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketType.Ticket;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.user.UserConfigType.UserConfig;
import org.openbase.type.timing.IntervalType;
import org.openbase.type.timing.IntervalType.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class ColorableLightRemoteWithAuthenticationTest extends AbstractBCODeviceManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorableLightRemoteWithAuthenticationTest.class);

    private static ColorableLightRemote colorableLightRemote;

    private final SessionManager adminSessionManager;
    private AuthToken adminToken = null;

    public ColorableLightRemoteWithAuthenticationTest() {
        adminSessionManager = new SessionManager();
    }

    @BeforeAll
    public static void setUpClass() throws Throwable {
        JPService.registerProperty(JPAuthentication.class, true);
        AbstractBCODeviceManagerTest.setUpClass();
    }

    @BeforeEach
    public void setUp() throws CouldNotPerformException, InterruptedException, ExecutionException {

        adminSessionManager.loginUser(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD, false);

        if (adminToken == null) {
            adminToken = TokenGenerator.generateAuthToken(adminSessionManager);
        }

        colorableLightRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.COLORABLE_LIGHT), true, ColorableLightRemote.class);
        colorableLightRemote.setSessionManager(adminSessionManager);
    }

    @AfterEach
    public void tearDown() throws CouldNotPerformException, ExecutionException, InterruptedException {
        adminSessionManager.logout();
        colorableLightRemote.setSessionManager(SessionManager.getInstance());
    }

    /**
     * Test if executing an action works with prior user login and authentication.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetColorWithAuthentication() throws Exception {
        System.out.println("testSetColorWithAuthentication");

        waitForExecution(colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON), adminToken);
        colorableLightRemote.requestData().get();
        assertEquals(State.ON, colorableLightRemote.getData().getPowerState().getValue(), "Power has not been set in time!");
    }

    /**
     * Test if executing an action works with prior client login and authentication, but then with an expired session.
     * So first, the relog should be executed for a client and then the action should be executed.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetColorWithClientWithoutAuthentication() throws Exception {
        System.out.println("testSetColorWithClientWithoutAuthentication");

        UnitConfig.Builder testClient = UnitConfig.newBuilder();
        testClient.setUnitType(UnitType.USER);
        UserConfig.Builder userConfig = testClient.getUserConfigBuilder();
        userConfig.setUserName("UnitTestClient");
        userConfig.setFirstName("First");
        userConfig.setLastName("Last");
        testClient.getPermissionConfigBuilder().getOtherPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        UnitConfig registered = Registries.getUnitRegistry().registerUnitConfig(testClient.build()).get();

        // register client
        System.out.println("register client");
        adminSessionManager.registerClient(registered.getId()).get();

        // logout admin
        System.out.println("logout admin");
        adminSessionManager.logout();

        // login client
        System.out.println("login client");
        adminSessionManager.loginClient(registered.getId(), false);

        // make ticket invalid
        byte[] serviceServerSecretKey = AuthenticatedServerManager.getInstance().getServiceServerSecretKey();
        System.out.println("make ticket invalid");
        Ticket ticket = EncryptionHelper.decryptSymmetric(adminSessionManager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);

        long currentTime = 0;
        Interval.Builder validityInterval = IntervalType.Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + JPService.getProperty(JPSessionTimeout.class).getValue()));
        validityInterval.build();

        Ticket.Builder cstb = ticket.toBuilder();
        cstb.setValidityPeriod(validityInterval.build());

        TicketAuthenticatorWrapper.Builder wrapperBuilder = adminSessionManager.getTicketAuthenticatorWrapper().toBuilder();
        wrapperBuilder.setTicket(EncryptionHelper.encryptSymmetric(cstb.build(), serviceServerSecretKey));

        adminSessionManager.updateTicketAuthenticatorWrapper(wrapperBuilder.build());

        // execute action
        System.out.println("execute action");
        waitForExecution(colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON), adminToken);
        colorableLightRemote.requestData().get();
        assertEquals(State.ON, colorableLightRemote.getData().getPowerState().getValue(), "Power has not been set in time!");
    }

    /**
     * Test if executing an action works with prior user login and authentication, but then with an expired session.
     * So first, the relog should be executed for a user and then the action should be executed.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetColorWithUserWithoutAuthentication() throws Exception {
        System.out.println("testSetColorWithClientWithoutAuthentication");

        byte[] serviceServerSecretKey = AuthenticatedServerManager.getInstance().getServiceServerSecretKey();
        // make ticket invalid
        System.out.println("make ticket invalid");
        Ticket ticket = EncryptionHelper.decryptSymmetric(adminSessionManager.getTicketAuthenticatorWrapper().getTicket(), serviceServerSecretKey, Ticket.class);

        long currentTime = 0;
        Interval.Builder validityInterval = IntervalType.Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + JPService.getProperty(JPSessionTimeout.class).getValue()));
        validityInterval.build();

        Ticket.Builder cstb = ticket.toBuilder();
        cstb.setValidityPeriod(validityInterval.build());

        TicketAuthenticatorWrapper.Builder wrapperBuilder = adminSessionManager.getTicketAuthenticatorWrapper().toBuilder();
        wrapperBuilder.setTicket(EncryptionHelper.encryptSymmetric(cstb.build(), serviceServerSecretKey));

        adminSessionManager.updateTicketAuthenticatorWrapper(wrapperBuilder.build());

        // execute action
        System.out.println("execute action");
        waitForExecution(colorableLightRemote.setPowerState(PowerStateType.PowerState.State.ON), adminToken);
        colorableLightRemote.requestData().get();
        assertEquals(State.ON, colorableLightRemote.getData().getPowerState().getValue(), "Power has not been set in time!");
    }

    @Test
    @Timeout(15)
    public void testApplyActionWithToken() throws Exception {
        System.out.println("testApplyActionWithToken");

        // Remove other access permission from root location so that new users should not be able to control it
        final UnitConfig.Builder rootLocation = Registries.getUnitRegistry().getRootLocationConfig().toBuilder();
        rootLocation.getPermissionConfigBuilder().getOtherPermissionBuilder().setAccess(false);
        Registries.getUnitRegistry().updateUnitConfig(rootLocation.build()).get();

        // Register a new user that does not have access permissions
        final String username = "Murray";
        final String password = "skull";
        UnitConfig.Builder userUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.USER);
        userUnitConfig.getUserConfigBuilder().setFirstName("Murray").setLastName("the skull").setUserName(username);
        userUnitConfig = Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get().toBuilder();
        adminSessionManager.registerUser(userUnitConfig.getId(), password, false).get();

        // request authentication and authorization tokens for admin user
        AuthenticatedValue authenticatedValue = adminSessionManager.initializeRequest(AuthenticationToken.newBuilder().setUserId(adminSessionManager.getUserClientPair().getUserId()).build(), null);
        final String authenticationToken = new AuthenticatedValueFuture<>(
                Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                String.class,
                authenticatedValue.getTicketAuthenticatorWrapper(),
                adminSessionManager).get();
        AuthorizationToken.Builder authorizationToken = AuthorizationToken.newBuilder().setUserId(adminSessionManager.getUserClientPair().getUserId());
        AuthorizationToken.PermissionRule.Builder permissionRuleBuilder = authorizationToken.addPermissionRuleBuilder();
        permissionRuleBuilder.setUnitId(colorableLightRemote.getId());
        permissionRuleBuilder.getPermissionBuilder().setAccess(true).setRead(true).setWrite(false);
        authenticatedValue = adminSessionManager.initializeRequest(authorizationToken.build(), null);
        final AuthToken.Builder token = AuthToken.newBuilder().setAuthorizationToken(new AuthenticatedValueFuture<>(
                Registries.getUnitRegistry().requestAuthorizationTokenAuthenticated(authenticatedValue),
                String.class,
                authenticatedValue.getTicketAuthenticatorWrapper(),
                adminSessionManager).get());

        // login previously registered user
        adminSessionManager.loginUser(userUnitConfig.getId(), password, false);

        // try to set the power state which should fail
        try {
            ExceptionPrinter.setBeQuit(true);
            colorableLightRemote.setPowerState(State.ON).get();
            assertTrue(false, "Could set power state without access permissions");
        } catch (ExecutionException ex) {
            // this should happen
            // ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.INFO);
        } finally {
            ExceptionPrinter.setBeQuit(false);
        }

        PowerState.Builder powerState = PowerState.newBuilder().setValue(State.ON);
        ActionDescription actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(powerState.build(), ServiceType.POWER_STATE_SERVICE, colorableLightRemote).build();

        authenticatedValue = adminSessionManager.initializeRequest(actionDescription, token.build());
        AuthenticatedValueFuture<ActionDescription> future = new AuthenticatedValueFuture<>(colorableLightRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), adminSessionManager);
        waitForExecution(future, adminToken);
        assertEquals(State.ON, colorableLightRemote.getPowerState().getValue());

        colorableLightRemote.cancelAction(actionDescription);
        try {
            authenticatedValue = adminSessionManager.initializeRequest(future.get().toBuilder().setCancel(true).build(), token.build());
            new AuthenticatedValueFuture<>(colorableLightRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), adminSessionManager).get();
            LOGGER.info("Successfully canceled action: " + future.get().getActionId() + " - " + MultiLanguageTextProcessor.getBestMatch(future.get().getDescription()));
        } catch (ExecutionException ex) {
            LOGGER.error("Could not cancel action!", ex);
            throw ex;
        }

        token.setAuthenticationToken(authenticationToken);
        Future<ActionDescription> f = colorableLightRemote.setPowerState(State.OFF, ActionParameterType.ActionParameter.newBuilder().setAuthToken(token.build()).build());
        waitForExecution(f, adminToken);
        assertEquals(State.OFF, colorableLightRemote.getPowerState().getValue());

        try {
            colorableLightRemote.cancelAction(f.get(), token.build()).get();
        } catch (ExecutionException ex) {
            LOGGER.error("Could not cancel action!", ex);
            throw ex;
        }

        // reset root location permissions to not interfere with other tests
        rootLocation.getPermissionConfigBuilder().getOtherPermissionBuilder().setAccess(true);
        Registries.getUnitRegistry().updateUnitConfig(rootLocation.build()).get();
    }

    @Test
    @Timeout(15)
    public void testApplyActionViaServiceRemoteWithToken() throws Exception {
        System.out.println("testApplyActionViaServiceRemoteWithToken");

        // grand permissions
        BCOLogin.getSession().loginUserViaUsername("admin", "admin", false);

        // Register a new user
        final String username = "Largo";
        final String password = "Money";
        UnitConfig.Builder largoUserUnitConfig = UnitConfig.newBuilder().setUnitType(UnitType.USER);
        largoUserUnitConfig.getUserConfigBuilder().setFirstName("Largo").setLastName("LaGrande").setUserName(username);

        largoUserUnitConfig = Registries.getUnitRegistry().registerUnitConfig(largoUserUnitConfig.build()).get().toBuilder();
        BCOLogin.getSession().getSessionManager().registerUser(largoUserUnitConfig.getId(), password, false).get();

        // request token for new user and build default param
        BCOLogin.getSession().loginUserViaUsername(username, password, false);
        final AuthToken largosAuthToken = BCOLogin.getSession().generateAuthToken();
        final ActionParameter largosDefaultParameter = ActionParameter.newBuilder().setAuthToken(largosAuthToken).build();

        // logout so no one is logged in.
        BCOLogin.getSession().logout();


        PowerStateServiceRemote powerStateServiceRemote = new PowerStateServiceRemote();
        powerStateServiceRemote.init(colorableLightRemote.getConfig());
        powerStateServiceRemote.activate(true);

        System.out.println("first largo control");

        Future<ActionDescription> future = powerStateServiceRemote.setPowerState(Power.ON, largosDefaultParameter);
        waitForExecution(future, largosAuthToken);
        System.out.println("end largo control");

        // make sure sync is done
        powerStateServiceRemote.requestData().get(5 , TimeUnit.SECONDS);

        // validate state
        assertEquals(State.ON, powerStateServiceRemote.getPowerState().getValue());

        // validate authority
        assertEquals(largoUserUnitConfig.getId(), powerStateServiceRemote.getPowerState().getResponsibleAction().getActionInitiator().getInitiatorId());

        powerStateServiceRemote.cancelAction(future.get(), largosAuthToken).get(5 , TimeUnit.SECONDS);

        future = powerStateServiceRemote.setPowerState(Power.OFF, largosDefaultParameter);
        waitForExecution(future, largosAuthToken);

        // make sure sync is done
        powerStateServiceRemote.requestData().get(5 , TimeUnit.SECONDS);

        assertEquals(State.OFF, powerStateServiceRemote.getPowerState().getValue());
        assertEquals(largoUserUnitConfig.getId(), powerStateServiceRemote.getPowerState().getResponsibleAction().getActionInitiator().getInitiatorId());

        try {
            powerStateServiceRemote.cancelAction(future.get(5 , TimeUnit.SECONDS), largosAuthToken).get(5 , TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            LOGGER.error("Could not cancel action!", ex);
            throw ex;
        }

        // make sure sync is done
        powerStateServiceRemote.requestData().get(5 , TimeUnit.SECONDS);

        // make sure largo is not responsible any more for the current state.
        assertNotEquals(largoUserUnitConfig.getId(), powerStateServiceRemote.getPowerState().getResponsibleAction().getActionInitiator().getInitiatorId());
    }
}
