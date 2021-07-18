package org.openbase.bco.dal.test.action;

/*-
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

import org.junit.*;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.session.TokenGenerator;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.action.ActionReferenceType;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.state.ActionStateType;
import org.openbase.type.domotic.state.ActionStateType.ActionState;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

public class RemoteActionTest extends AbstractBCOLocationManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteActionTest.class);

    private static AuthToken mrPinkUserToken;
    private static String mrPinkUserId;
    private static ActionParameterType.ActionParameter mrPinkActionParameter;

    public RemoteActionTest() {

        // uncomment to enable debug mode
        // JPService.registerProperty(JPDebugMode.class, true);
        // JPService.registerProperty(JPLogLevel.class, LogLevel.DEBUG);

        // uncomment to visualize action inspector during tests
        /*String[] args = {};
        new Thread(() -> {
            try {
                Registries.waitForData();
                BCOActionInspector.main(args);
            } catch (CouldNotPerformException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();*/
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCOLocationManagerTest.setUpClass();

        // create new user token for test
        try {
            // login as admin
            final SessionManager sessionManager = new SessionManager();
            sessionManager.loginUser(Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.ADMIN_USER_ALIAS).getId(), UserCreationPlugin.ADMIN_PASSWORD, false);

            // register a new user which is not an admin
            final String username = "Undercover";
            final String password = "Agent";
            UnitConfigType.UnitConfig.Builder userUnitConfig = UnitConfigType.UnitConfig.newBuilder().setUnitType(UnitType.USER);
            userUnitConfig.getUserConfigBuilder().setFirstName("Mr").setLastName("Pink").setUserName(username);
            mrPinkUserId = Registries.getUnitRegistry().registerUnitConfig(userUnitConfig.build()).get().getId();
            sessionManager.registerUser(mrPinkUserId, password, false).get();

            // login new user
            sessionManager.logout();
            sessionManager.loginUser(mrPinkUserId, password, false);

            // request authentication token for new user
            mrPinkUserToken = TokenGenerator.generateAuthToken(sessionManager);
            mrPinkActionParameter = ActionParameterType.ActionParameter.newBuilder().setAuthToken(mrPinkUserToken).build();

            // logout user
            sessionManager.logout();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    @Test(timeout = 10000)
    public void testExecutionAndCancellationWithToken() throws Exception {
        System.out.println("testExecutionAndCancellationWithToken");

        final LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);
        List<? extends ColorableLightRemote> units = locationRemote.getUnits(UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT);

        for (int i = 0; i < 10; i++) {
            PowerStateType.PowerState.State powerState = (i % 2 == 0) ? PowerStateType.PowerState.State.ON : PowerStateType.PowerState.State.OFF;
            final RemoteAction locationRemoteAction = waitForExecution(locationRemote.setPowerState(powerState, UnitType.COLORABLE_LIGHT, mrPinkActionParameter), mrPinkUserToken);
            assertTrue("Action of location does not offer an id after submission!", !locationRemoteAction.getId().isEmpty());

            for (ColorableLightRemote unit : units) {
                boolean actionIsExecuting = false;
                for (ActionReferenceType.ActionReference actionReference : unit.getActionList().get(0).getActionCauseList()) {
                    assertTrue("Subaction of location does not offer an id after submission!", !actionReference.getActionId().isEmpty());
                    if (actionReference.getActionId().equals(locationRemoteAction.getId())) {
                        actionIsExecuting = true;
                        break;
                    }
                }

                assertTrue("Action on unit[" + unit + "] is not executing", actionIsExecuting);
                assertEquals("Action was not authenticated by the correct user", mrPinkUserId, unit.getActionList().get(0).getActionInitiator().getAuthenticatedBy());
            }

            locationRemoteAction.cancel().get();

            // validate that action is cancelled on all units
            for (final ColorableLightRemote colorableLightRemote : units) {
//                System.out.println("process: " + colorableLightRemote);
                ActionDescriptionType.ActionDescription causedAction = null;
                for (final ActionDescriptionType.ActionDescription description : colorableLightRemote.getActionList()) {
//                    System.out.println("    action: " + ActionDescriptionProcessor.toString(description));
                    for (ActionReferenceType.ActionReference cause : description.getActionCauseList()) {
//                        System.out.println("        reference: " + ActionDescriptionProcessor.toString(cause));
//                        System.out.println("        compare  : " + locationRemoteAction.getId());
                        if (cause.getActionId().equals(locationRemoteAction.getId())) {
                            causedAction = description;
//                            System.out.println("            match: " + ActionDescriptionProcessor.toString(causedAction));
                            break;
                        }
                    }
                }

                if (causedAction == null) {
                    fail("Caused action on unit[" + colorableLightRemote + "] could not be found!");
                }

                Assert.assertEquals("Action on unit[" + colorableLightRemote + "] was not cancelled!", ActionStateType.ActionState.State.CANCELED, causedAction.getActionState().getValue());
            }

        }
    }

    @Test(timeout = 10000)
    public void testExtentionCancelation() throws Exception {
        System.out.println("testExtentionCancelation");

        final LocationRemote locationRemote = Units.getUnit(Registries.getUnitRegistry().getRootLocationConfig(), true, Units.LOCATION);

        // apply low prio action
        System.out.println("apply low prio action...");
        Flag lowPrioLongtermActionExtentionFlag = new Flag();
        final RemoteAction lowPrioLongtermAction = new RemoteAction(locationRemote.setPowerState(State.OFF, ActionParameter.newBuilder().setPriority(Priority.LOW).setSchedulable(true).setInterruptible(true).setExecutionTimePeriod(TimeUnit.MINUTES.toMicros(30)).build()), new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                System.out.println("low prio action is extended");
                lowPrioLongtermActionExtentionFlag.setValue(true);
                return true;
            }
        });

        observe(lowPrioLongtermAction);

        System.out.println("wait for low prio action...");
        waitForRegistration(lowPrioLongtermAction);

        System.out.println("apply normal prio action...");
        final Flag dominantActionExtentionFlag = new Flag();


        final RemoteAction dominantAction = new RemoteAction(locationRemote.setPowerState(State.ON, mrPinkActionParameter), mrPinkUserToken, () -> {
            System.out.println("dominant action is extended");
            dominantActionExtentionFlag.setValue(true);
            return true;
        });

        System.out.println("wait for normal prio action...");
        waitForExecution(dominantAction);

        System.out.println("validate light state ON");
        final List<? extends ColorableLightRemote> units = locationRemote.getUnits(UnitType.COLORABLE_LIGHT, true, Units.COLORABLE_LIGHT);

        for (ColorableLightRemote unit : units) {
            unit.requestData().get();
            Assert.assertEquals("Light[" + unit + "] not on", State.ON, unit.getPowerState().getValue());
        }

        System.out.println("cancel dominant action");
        dominantAction.cancel().get();
        dominantActionExtentionFlag.setValue(false);

        System.out.println("validate light state OFF");

        // hopefully outdated since we can wait for the execution of the low prio action.
//        while (true) {
//            boolean success = true;
//            for (ColorableLightRemote unit : units) {
//                unit.requestData().get();
//                if (unit.getPowerState().getValue() != State.OFF) {
//                    success = false;
//                    LOGGER.warn("Light[" + unit + "] not off");
//                }
//            }
//
//            if(success) {
//                break;
//            }
//            Thread.sleep(1000);
//        }

        lowPrioLongtermAction.waitForActionState(ActionState.State.EXECUTING);

        for (ColorableLightRemote unit : units) {
            unit.requestData().get();
            Assert.assertEquals("Light[" + unit + "] not off", State.OFF, unit.getPowerState().getValue());
        }

        System.out.println("wait until last extension timeout and validate if no further extension will be performed for dominant action...");
        Thread.sleep(dominantAction.getValidityTime(TimeUnit.MILLISECONDS) + 10);
        Assert.assertEquals("Dominant action was extended even through the action was already canceled.", false, dominantActionExtentionFlag.value);

        System.out.println("cancel low prio action");
        lowPrioLongtermAction.cancel().get();
        lowPrioLongtermActionExtentionFlag.setValue(false);

        System.out.println("validate everything is done");
        for (ColorableLightRemote unit : units) {
            unit.requestData().get();
            for (ActionDescription actionDescription : unit.getActionList()) {

                // filter termination action
                if (actionDescription.getPriority() == Priority.TERMINATION) {
                    continue;
                }

                Assert.assertEquals("Zombie[" + actionDescription.getActionState().getValue().name() + "] actions detected: " + MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription()), true, new RemoteAction(actionDescription).isDone());
            }
        }

        System.out.println("wait until last extension timeout and validate if no further extension will be performed for low prio longterm action...");
        Thread.sleep(lowPrioLongtermAction.getValidityTime(TimeUnit.MILLISECONDS) + 10);
        Assert.assertEquals("Low prio action was extended even through the action was already canceled.", false, lowPrioLongtermActionExtentionFlag.value);

        System.out.println("validate if still everything is done");
        for (ColorableLightRemote unit : units) {
            for (ActionDescription actionDescription : unit.getActionList()) {

                // filter termination action
                if (actionDescription.getPriority() == Priority.TERMINATION) {
                    continue;
                }

                Assert.assertEquals("Zombie[" + actionDescription.getActionState().getValue().name() + "] actions detected: " + MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription()), true, new RemoteAction(actionDescription).isDone());
            }
        }

        System.out.println("test successful");

    }

    class Flag {
        private volatile boolean value = false;

        public void setValue(boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }
    }
}
