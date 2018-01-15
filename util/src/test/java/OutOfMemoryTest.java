/*-
 * #%L
 * BCO Manager Utility
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.app.AppRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.util.launch.BCOLauncher;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.state.ActivationStateType.ActivationState.State;
import rst.domotic.state.PowerStateType.PowerState;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*-
 * #%L
 * BCO Manager Utility
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

public class OutOfMemoryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutOfMemoryTest.class);

    @BeforeClass
    public static void setUpClass() throws Throwable {
        JPService.setupJUnitTestMode();
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
    }

    // @Test
    public void testOutOfMemory() throws Exception {
        String[] args = {"-d", "--force", "-v", "--prefix", "/home/liquid/release/", "--benchmark"};
        BCOLauncher.main(args);

        // wait 30s for startup
        Thread.sleep(30 * 1000);

        AppRemote appRemote = Units.getUnitsByLabel("PartyLightFollower", true, AppRemote.class).get(0);
        appRemote.setActivationState(State.ACTIVE).get();

        // wait for the app to trigger some lights
        Thread.sleep(30 * 1000);

        // deactivate app
//        appRemote.setActivationState(State.DEACTIVE).get();

        // create remote for root location
        LocationRemote rootLocation = Units.getUnit(Registries.getLocationRegistry().getRootLocationConfig(), true, LocationRemote.class);

        // record first snapshot
        Snapshot firstSnapshot = rootLocation.recordSnapshot().get();

        // turn of everything
        rootLocation.setPowerState(PowerState.State.ON).get();

        // record second snapshot
        Snapshot secondSnapshot = rootLocation.recordSnapshot().get();

        // reactivate party app:
//        appRemote.setActivationState(State.ACTIVE).get();

        LOGGER.info("Snapshot1:\n" + firstSnapshot + "\nSnapshot2\n" + secondSnapshot);

        while (true) {
            Thread.sleep(5 * 1000);
//            try {
//                LOGGER.info("\n================================================================" +
//                        "\n            Execute first snapshot                              " +
//                        "\n================================================================");
//                rootLocation.restoreSnapshot(firstSnapshot).get(30, TimeUnit.SECONDS);
//                LOGGER.info("\n================================================================" +
//                        "\n            Finished executing first snapshot                   " +
//                        "\n================================================================");
//            } catch (ExecutionException | TimeoutException ex) {
//                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not restore first snapshot", ex), LOGGER);
//            }
//            Thread.sleep(5 * 1000);
//            try {
//                LOGGER.info("\n================================================================" +
//                        "\n            Execute second snapshot                              " +
//                        "\n================================================================");
//                rootLocation.restoreSnapshot(secondSnapshot).get(30, TimeUnit.SECONDS);
//
//                LOGGER.info("\n================================================================" +
//                        "\n            Finished executing second snapshot                   " +
//                        "\n================================================================");
//            } catch (ExecutionException | TimeoutException ex) {
//                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not restore second snapshot", ex), LOGGER);
//            }
        }
    }

    @Test
    public void testScheduleExecution() throws Exception {
        System.out.println("testScheduleExecution");

        printStats();
        GlobalScheduledExecutorService.schedule(new SleepingCallable(500, 1), 1, TimeUnit.SECONDS);
        GlobalScheduledExecutorService.schedule(new SleepingCallable(2000, 2), 1, TimeUnit.SECONDS);
        for(int i = 0; i < 50; i++) {
            GlobalScheduledExecutorService.schedule(new SleepingCallable(500, i+2), i, TimeUnit.SECONDS);
        }

        Thread.sleep(250);
        printStats();
        Thread.sleep(1000);
        printStats();
        Thread.sleep(2000);
        printStats();
        Thread.sleep(3600000);
    }

    public void printStats() {
        long completedTaskCount = GlobalScheduledExecutorService.getInstance().getExecutorService().getCompletedTaskCount();
        long taskCount = GlobalScheduledExecutorService.getInstance().getExecutorService().getTaskCount();
        int activeCount = GlobalScheduledExecutorService.getInstance().getExecutorService().getActiveCount();
        System.out.println("Tasks[" + taskCount + "], completed[" + completedTaskCount + "], active[" + activeCount + "]");
    }

    private class SleepingCallable implements Callable<Void> {

        private final long time;
        private final int number;

        public SleepingCallable(long time, int number) {
            this.time = time;
            this.number = number;
        }

        @Override
        public Void call() throws Exception {
            System.out.println("Task " + number);
            try {
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }
}
