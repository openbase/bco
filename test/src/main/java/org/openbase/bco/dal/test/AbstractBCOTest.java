package org.openbase.bco.dal.test;

/*-
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

import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;

/**
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class AbstractBCOTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractBCOTest.class);

    protected static MockRegistry mockRegistry;

    private final List<RemoteAction> testActions = new ArrayList<>();

    public static void setUpClass() throws Throwable {
        try {
            JPService.setupJUnitTestMode();
            mockRegistry = MockRegistryHolder.newMockRegistry();
            Units.reinitialize();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    public static void tearDownClass() throws Throwable {
        try {
            Units.reset(AbstractBCOTest.class);
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    /**
     * Method is automatically called after each test run and there is no need to call it manually.
     * If you want to cancel all actions manually please use method {@code cancelAllTestActions()} to get feedback about the cancellation process.
     */
    @After
    public void autoCancelActionsAfterTestRun() {
        try {
            cancelAllTestActions();
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not cancel all test actions of test suite: " + getClass().getName(), ex, LOGGER);
        }
    }

    /**
     * This method can be used to register an action during the unit test.
     * This method blocks until the action is executed. Be aware that this can take a while if a higher ranked action is currently allocating the unit.
     * After the test run is done, all registered actions are canceled automatically to avoid interferences between different test runs.
     *
     * @param actionFuture the action to observe
     *
     * @return a remote action instance which can be used to observe the action state.
     *
     * @throws CouldNotPerformException is thrown if the action could not be observed.
     * @throws InterruptedException     is throw if the current thread was interrupted. This e.g. happens if the test timed out.
     */
    public RemoteAction waitForExecution(final Future<ActionDescription> actionFuture) throws CouldNotPerformException, InterruptedException {
        final RemoteAction testAction = observe(actionFuture);
        testAction.waitForActionState(State.EXECUTING);
        return testAction;
    }

    /**
     * This method can be used to register an action during the unit test.
     * This method blocks until the action is executed. Be aware that this can take a while if a higher ranked action is currently allocating the unit.
     * After the test run is done, all registered actions are canceled automatically to avoid interferences between different test runs.
     *
     * @param remoteAction the action to observe
     *
     * @return a remote action instance which can be used to observe the action state.
     *
     * @throws CouldNotPerformException is thrown if the action could not be observed.
     * @throws InterruptedException     is throw if the current thread was interrupted. This e.g. happens if the test timed out.
     */
    public RemoteAction waitForExecution(final RemoteAction remoteAction) throws CouldNotPerformException, InterruptedException {
        final RemoteAction testAction = observe(remoteAction);
        testAction.waitForActionState(State.EXECUTING);
        return testAction;
    }

    /**
     * This method can be used to register an action during the unit test.
     * This method blocks until the action is registered.
     * After the test run is done, all registered actions are canceled automatically to avoid interferences between different test runs.
     *
     * @param actionFuture the action to observe
     *
     * @return a remote action instance which can be used to observe the action state.
     *
     * @throws CouldNotPerformException is thrown if the action could not be observed.
     * @throws InterruptedException     is throw if the current thread was interrupted. This e.g. happens if the test timed out.
     */
    public RemoteAction waitForRegistration(final Future<ActionDescription> actionFuture) throws CouldNotPerformException, InterruptedException {
        final RemoteAction testAction = observe(actionFuture);
        testAction.waitForRegistration();
        return testAction;
    }

    /**
     * This method can be used to register an action during the unit test.
     * This method blocks until the action is registered.
     * After the test run is done, all registered actions are canceled automatically to avoid interferences between different test runs.
     *
     * @param remoteAction the action to observe
     *
     * @return a remote action instance which can be used to observe the action state.
     *
     * @throws CouldNotPerformException is thrown if the action could not be observed.
     * @throws InterruptedException     is throw if the current thread was interrupted. This e.g. happens if the test timed out.
     */
    public RemoteAction waitForRegistration(final RemoteAction remoteAction) throws CouldNotPerformException, InterruptedException {
        final RemoteAction testAction = observe(remoteAction);
        testAction.waitForRegistration();
        return testAction;
    }

    /**
     * This method can be used to register an action during the unit test.
     * After the test run is done, all registered actions are canceled automatically to avoid interferences between different test runs.
     *
     * @param actionFuture the action to observe
     *
     * @return a remote action instance which can be used to observe the action state.
     */
    public RemoteAction observe(final Future<ActionDescription> actionFuture) {
        return observe(new RemoteAction(actionFuture));
    }

    /**
     * This method can be used to register an action during the unit test.
     * After the test run is done, all registered actions are canceled automatically to avoid interferences between different test runs.
     *
     * @param remoteAction the remote action to observe
     *
     * @return a remote action instance which can be used to observe the action state.
     */
    public RemoteAction observe(final RemoteAction remoteAction) {

        // register current action.
        testActions.add(remoteAction);

        // cleanup finished actions
        for (RemoteAction testAction : new ArrayList<>(testActions)) {
            if (testAction.isDone()) {
                testActions.remove(testAction);
            }
        }

        return remoteAction;
    }

    /**
     * Method can be used to cancel all currently observed test actions.
     * Be aware that this method is automatically called after each test run.
     *
     * @throws CouldNotPerformException thrown if the cancellation fails.
     * @throws InterruptedException     is throw if the cancellation was interrupted.
     */
    public void cancelAllTestActions() throws CouldNotPerformException, InterruptedException {

        if (testActions.size() > 0) {
            LOGGER.info("Cancel " + testActions.size() + " ongoing test action"+(testActions.size() == 1 ? "" : "s")+" ...");
        }

        final List<Future<?>> cancelTasks = new ArrayList<>();

        for (RemoteAction testAction : testActions) {
            cancelTasks.add(testAction.cancel());
        }

        try {
            for (Future<?> cancelTask : cancelTasks) {
                cancelTask.get();
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not wait for at least on test action!");
        }

        testActions.clear();
    }
}
