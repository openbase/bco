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
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.state.States.Power;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.PowerSwitchRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.test.layer.unit.device.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerSwitchRemoteTest extends AbstractBCODeviceManagerTest {

    private static PowerSwitchRemote powerSwitchRemote;

    public PowerSwitchRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        powerSwitchRemote = Units.getUnitByAlias(MockRegistry.getUnitAlias(UnitType.POWER_SWITCH), true, PowerSwitchRemote.class);
    }

    /**
     * Test of setPowerState method, of class PowerPlugRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetPowerState() throws Exception {
        System.out.println("testSetPowerState");
        waitForExecution(powerSwitchRemote.setPowerState(Power.ON));
        assertEquals("Power state has not been set in time!", Power.ON.getValue(), powerSwitchRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of getPowerState method, of class PowerPlugRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPowerState() throws Exception {
        System.out.println("testGetPowerState");

        // apply service state
        final UnitController<?, ?> unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(powerSwitchRemote.getId());
        unitController.applyServiceState(Power.OFF, ServiceType.POWER_STATE_SERVICE);

        // force sync
        powerSwitchRemote.requestData().get();

        // validate service state
        assertEquals("Switch has not been set in time.", Power.OFF.getValue(), powerSwitchRemote.getPowerState().getValue());

        // apply service state
        unitController.applyServiceState(Power.ON, ServiceType.POWER_STATE_SERVICE);

        // force sync
        powerSwitchRemote.requestData().get();

        // validate service state
        assertEquals("Switch has not been set in time.", Power.ON.getValue(), powerSwitchRemote.getPowerState().getValue());
    }

    /**
     * Test bco performance.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 15000)
    public void testPowerStateServicePerformance() throws Exception {
        System.out.println("testPowerStateServicePerformance");

        PowerState powerState = null;
        final ActionParameter parameter = ActionParameter.newBuilder().setExecutionTimePeriod(100000000).build();

        for (int i = 0; i < 100; i++) {
            if ((i & 1) == 0) {
                // even
                powerState = Power.ON;
            } else {
                // odd
                powerState = Power.OFF;
            }
            // to not observe in order to speedup the calls
            powerSwitchRemote.setPowerState(powerState, parameter);
        }

        // make sure unit is still responding
        try {
            powerSwitchRemote.requestData().get(1, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            assertTrue("PowerSwitch did not response in time after massive load!", true);
        }

        // invert state
        powerState = powerState == Power.ON ? Power.OFF : Power.ON;

        final RemoteAction testAction = observe(powerSwitchRemote.setPowerState(powerState), true);

        try {
            testAction.waitForActionState(State.EXECUTING);
        } catch (CancellationException ex) {
            Assert.fail("Power action is not executing and instead: "+testAction.getActionState().name());
        }

        // make sure the final state is correctly applied.
        try {
            assertEquals(powerState.getValue(), powerSwitchRemote.requestData().get(1, TimeUnit.SECONDS).getPowerState().getValue());
        } catch (TimeoutException ex) {
            assertTrue("PowerSwitch did not response in time after massive load!", true);
        }

        // manually cancel all action
        for (ActionDescription actionDescription : powerSwitchRemote.getActionList()) {
            powerSwitchRemote.cancelAction(actionDescription).get();
        }
    }

    /**
     * Test bco performance.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 15000)
    public void testPowerStateServiceCancellationPerformance() throws Exception {
        System.out.println("testPowerStateServiceCancellationPerformance");

        final Random random = new Random();
        final ActionParameter parameter = ActionParameter.newBuilder().setExecutionTimePeriod(100000000).build();
        final ArrayList<RemoteAction> actionList = new ArrayList<>();
        final ArrayList<Future> submissionTask = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            submissionTask.add(GlobalCachedExecutorService.submit(() -> {
                final PowerState powerState;
                if (random.nextBoolean()) {
                    // even
                    powerState = Power.ON;
                } else {
                    // odd
                    powerState = Power.OFF;
                }
                actionList.add(observe(powerSwitchRemote.setPowerState(powerState, parameter)));
            }));
        }

        // wait until submission tasks are done
        for (Future future : submissionTask) {
            future.get(5, TimeUnit.SECONDS);
        }

        // wait for registration
        for (RemoteAction remoteAction : actionList) {
            remoteAction.waitForRegistration();
        }

        final ArrayList<Future<?>> cancelTaskList = new ArrayList<>();
        final List<Throwable> errorList = Collections.synchronizedList(new ArrayList<>());

        // cancel all actions in parallel
        for (RemoteAction remoteAction : actionList) {
            cancelTaskList.add(GlobalCachedExecutorService.submit(() -> {
                try {
                    remoteAction.cancel().get(5, TimeUnit.SECONDS);
                } catch (Throwable ex) {
                    errorList.add(ex);
                }
            }));
        }

        // wait for cancellation
        for (Future<?> future : cancelTaskList) {
            future.get(5, TimeUnit.SECONDS);
        }

        // analyse errors
        assertEquals("Some errors occured during cancelation!", Collections.EMPTY_LIST, errorList);

        // make sure unit is still responding
        try {
            powerSwitchRemote.requestData().get(1, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            assertTrue("PowerSwitch did not response in time after massive load!", true);
        }

        // wait until all actions are canceled
        for (RemoteAction remoteAction : new ArrayList<>(actionList)) {
            remoteAction.waitUntilDone();
        }

        // invert state
        final PowerState powerState = powerSwitchRemote.getPowerState().getValue() == Power.ON.getValue() ? Power.OFF : Power.ON;

        System.out.println("set powerstate");

        waitForExecution(powerSwitchRemote.setPowerState(powerState));

        // make sure the final state is correctly applied.
        try {
            assertEquals(powerState.getValue(), powerSwitchRemote.requestData().get(1, TimeUnit.SECONDS).getPowerState().getValue());
        } catch (TimeoutException ex) {
            assertTrue("PowerSwitch did not response in time after massive load!", true);
        }
    }
}
