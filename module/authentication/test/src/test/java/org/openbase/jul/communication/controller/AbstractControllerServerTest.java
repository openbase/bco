package org.openbase.jul.communication.controller;

/*
 * #%L
 * JUL Extension Controller
 * %%
 * Copyright (C) 2015 - 2022 openbase.org
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.jul.communication.iface.RPCServer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.StackTracePrinter;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.Stopwatch;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.openbase.type.domotic.state.AvailabilityStateType.AvailabilityState.State.OFFLINE;
import static org.openbase.type.domotic.state.AvailabilityStateType.AvailabilityState.State.ONLINE;
import static org.openbase.type.domotic.state.ConnectionStateType.ConnectionState.State.*;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AbstractControllerServerTest extends MqttIntegrationTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractControllerServerTest() {
    }

    private boolean firstSync = false;
    private boolean secondSync = false;
    private AbstractControllerServer<UnitRegistryData, UnitRegistryData.Builder> communicationService;

    /**
     * Test if the initial sync that happens if a communication service starts
     * successfully publishes its data to a remote.
     *
     * @throws Exception
     */
    @Timeout(5)
    @Test
    public void testInitialSync() throws Exception {
        String scope = "/test/synchronization";
        final SyncObject waitForDataSync = new SyncObject("WaitForDataSync");
        UnitConfig unit1 = UnitConfig.newBuilder().setId("Location1").build();
        UnitRegistryData.Builder testData = UnitRegistryData.getDefaultInstance().toBuilder().addLocationUnitConfig(unit1);
        communicationService = new AbstractControllerServerImpl(testData);
        communicationService.init(scope);
        communicationService.activate();

        AbstractRemoteClient<UnitRegistryData> remoteService = new AbstractRemoteClientImpl();
        remoteService.init(scope);
        remoteService.addDataObserver((source, data) -> {
            if (data.getLocationUnitConfigCount() == 1 && data.getLocationUnitConfig(0).getId().equals("Location1")) {
                firstSync = true;
                synchronized (waitForDataSync) {
                    waitForDataSync.notifyAll();
                }
            }
            if (data.getLocationUnitConfigCount() == 2 && data.getLocationUnitConfig(0).getId().equals("Location1") && data.getLocationUnitConfig(1).getId().equals("Location2")) {
                secondSync = true;
                synchronized (waitForDataSync) {
                    waitForDataSync.notifyAll();
                }
            }
        });

        synchronized (waitForDataSync) {
            if (!firstSync) {
                remoteService.activate();
                waitForDataSync.wait();
            }
        }
        assertTrue(firstSync, "Synchronization after the start of the remote service has not been done");

        communicationService.shutdown();
        UnitConfig location2 = UnitConfig.newBuilder().setId("Location2").build();
        testData.addLocationUnitConfig(location2);
        communicationService = new AbstractControllerServerImpl(testData);
        communicationService.init(scope);

        synchronized (waitForDataSync) {
            if (!secondSync) {
                communicationService.activate();
                waitForDataSync.wait();
            }
        }
        assertTrue(secondSync, "Synchronization after the restart of the communication service has not been done");

        communicationService.deactivate();
        try {
            remoteService.ping().get();
            fail("Pinging was not canceled after timeout.");
        } catch (ExecutionException ex) {
            // ping canceled
        }

        assertEquals(
                CONNECTING,
                remoteService.getConnectionState(),
                "Remote is still connected after remote service shutdown!");
        communicationService.activate();
        communicationService.waitForAvailabilityState(ONLINE);
        remoteService.waitForConnectionState(CONNECTED);
        remoteService.shutdown();
        communicationService.shutdown();
        assertEquals(
                OFFLINE,
                communicationService.getControllerAvailabilityState(),
                "Communication Service is not offline after shutdown!");
        assertEquals(
                DISCONNECTED,
                remoteService.getConnectionState(),
                "Remote is not disconnected after shutdown!");
    }

    /**
     * Test if a RemoteService will reconnect when the communication service
     * restarts.
     *
     * This test validates, that at least 10 re-connection cycles can be repeated
     * within a timeout of 5 seconds.
     */
    @Timeout(5)
    @Test
    public void testReconnection() throws Exception {
        int cycles = 10;
        String scope = "/test/reconnection";
        UnitConfig location1 = UnitConfig.newBuilder().setId("Location1").build();
        UnitRegistryData.Builder testData = UnitRegistryData.getDefaultInstance().toBuilder().addLocationUnitConfig(location1);

        communicationService = new AbstractControllerServerImpl(testData);
        communicationService.init(scope);
        communicationService.activate();

        AbstractRemoteClient<UnitRegistryData> remoteService = new AbstractRemoteClientImpl();
        remoteService.init(scope);
        remoteService.activate();

        remoteService.waitForConnectionState(CONNECTED);

        Stopwatch stopWatch = new Stopwatch();
        stopWatch.start();
        for (int i = 0; i < cycles; i++) {
            communicationService.deactivate();
            remoteService.waitForConnectionState(CONNECTING);

            communicationService.activate();
            remoteService.waitForConnectionState(CONNECTED);
        }
        logger.info(stopWatch.stop() + "ms for 100 reconnection cycles (" + (stopWatch.getTime() / cycles) + ")ms on average");

        communicationService.shutdown();
        remoteService.shutdown();
    }

    /**
     * Test waiting for data from a communication service.
     */
    @Timeout(5)
    @Test
    public void testWaitForData() throws Exception {
        String scope = "/test/waitfordata";
        UnitConfig location1 = UnitConfig.newBuilder().setId("Location1").build();
        UnitRegistryData.Builder testData = UnitRegistryData.getDefaultInstance().toBuilder().addLocationUnitConfig(location1);

        AbstractRemoteClient<UnitRegistryData> remoteService = new AbstractRemoteClientImpl();
        remoteService.init(scope);
        communicationService = new AbstractControllerServerImpl(testData);
        communicationService.init(scope);

        remoteService.activate();

        Future<?> dataFuture = remoteService.getDataFuture();

        communicationService.activate();

        assertEquals(
                communicationService.getData(),
                dataFuture.get(),
                "DataFuture did not return data from communicationService!");

        communicationService.shutdown();
        remoteService.shutdown();
    }

    /**
     * Test requesting data from a communication service.
     */
    @Timeout(5)
    @Test
    public void testRequestData() throws Exception {
        String scope = "/test/requestdata";
        UnitConfig location1 = UnitConfig.newBuilder().setId("Location1").build();
        UnitRegistryData.Builder testData = UnitRegistryData.getDefaultInstance().toBuilder().addLocationUnitConfig(location1);

        AbstractRemoteClient<UnitRegistryData> remoteService = new AbstractRemoteClientImpl();
        remoteService.init(scope);
        communicationService = new AbstractControllerServerImpl(testData);
        communicationService.init(scope);

        remoteService.activate();
        communicationService.activate();

        remoteService.requestData().get();

        assertEquals(
                communicationService.getData(),
                remoteService.getData(),
                "CommunicationService data and remoteService data do not match after requestData!");

        communicationService.shutdown();
        remoteService.shutdown();
    }

    /**
     * Test if when there are 2 remotes connected to a communication service
     * the shutdown of one remote affects the communication of the other one.
     */
    @Timeout(60)
    @Test
    public void testRemoteInterference() throws Exception {
        String scope = "/test/interference";
        UnitConfig location1 = UnitConfig.newBuilder().setId("Location1").build();
        UnitRegistryData.Builder testData = UnitRegistryData.getDefaultInstance().toBuilder().addLocationUnitConfig(location1);

        AbstractRemoteClient<UnitRegistryData> remoteService1 = new AbstractRemoteClientImpl();
        AbstractRemoteClient<UnitRegistryData> remoteService2 = new AbstractRemoteClientImpl();
        remoteService1.init(scope);
        remoteService2.init(scope);
        communicationService = new AbstractControllerServerImpl(testData);
        communicationService.init(scope);
        communicationService.activate();

        remoteService1.activate();
        remoteService2.activate();

        remoteService1.waitForConnectionState(CONNECTED);
        remoteService2.waitForConnectionState(CONNECTED);
        System.out.println("shutdown remote 1");
        remoteService1.shutdown();
        remoteService1.waitForConnectionState(DISCONNECTED);

        assertEquals(
                CONNECTED,
                remoteService2.getConnectionState(),
                "Remote connected to the same service got shutdown too");
        remoteService2.requestData().get();

        System.out.println("deactivate server");
        communicationService.deactivate();
        remoteService2.waitForConnectionState(CONNECTING);

        System.out.println("activate server");
        communicationService.activate();
        remoteService2.waitForConnectionState(CONNECTED);
        assertEquals(
                DISCONNECTED,
                remoteService1.getConnectionState(),
                "Remote reconnected even though it already shutdown");

        remoteService2.shutdown();
        communicationService.shutdown();
    }

    /**
     * @throws Exception
     */
    @Timeout(10)
    @Test
    @Disabled // Ignore test since it's pretty unstable.
    public void testNotification() throws Exception {
        String scope = "/test/notification";
        UnitConfig location = UnitConfig.newBuilder().setId("id").build();
        communicationService = new AbstractControllerServerImpl(UnitRegistryData.getDefaultInstance().toBuilder().addLocationUnitConfig(location));
        communicationService.init(scope);

        AbstractRemoteClient<UnitRegistryData> remoteService = new AbstractRemoteClientImpl();
        remoteService.init(scope);
        remoteService.activate();
        remoteService.waitForMiddleware();

        GlobalCachedExecutorService.submit(() -> {
            try {
                // make sure the remote is ready to wait for data
                remoteService.waitForConnectionState(CONNECTING);
                communicationService.activate();
                // notification should be sent automatically.
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(new FatalImplementationErrorException(this, ex), System.err);
            }
        });

        remoteService.waitForData();
        try {
            remoteService.ping().get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException ex) {
            StackTracePrinter.printAllStackTraces(LoggerFactory.getLogger(getClass()), LogLevel.WARN);
            fail("Even though wait for data returned the pinging immediately afterwards took to long. Please check stacktrace for deadlocks...");
        }

        remoteService.shutdown();
        communicationService.shutdown();
    }

    @Timeout(5)
    @Test
    public void testReinit() throws Exception {
        final int TEST_PARALLEL_REINIT_TASKS = 5;
        String scope = "/test/notification";
        UnitConfig location = UnitConfig.newBuilder().setId("id").build();
        communicationService = new AbstractControllerServerImpl(UnitRegistryData.newBuilder().addLocationUnitConfig(location));
        communicationService.init(scope);
        communicationService.activate();

        AbstractRemoteClient<UnitRegistryData> remoteService = new AbstractRemoteClientImpl();
        remoteService.init(scope);
        remoteService.activate();

        final Runnable reinitTask = () -> {
            try {
                remoteService.reinit();
                remoteService.requestData().get();
            } catch (Exception ex) {
                ExceptionPrinter.printHistory("reinit failed!", ex, logger);
            }
        };

        // execute reinits
        final ArrayList<Future<?>> taskFutures = new ArrayList<>();
        for (int i = 0; i < TEST_PARALLEL_REINIT_TASKS; i++) {
            taskFutures.add(GlobalCachedExecutorService.submit(reinitTask));
        }
        for (Future<?> future : taskFutures) {
            try {
                future.get(15, TimeUnit.SECONDS);
            } catch (TimeoutException ex) {
                //StackTracePrinter.printAllStackTrace(AbstractControllerServerTest.class);
                StackTracePrinter.detectDeadLocksAndPrintStackTraces(AbstractControllerServerTest.class);
                fail("Reint took too long! Please analyse deadlock in stacktrace...");
            }
        }

        remoteService.waitForData();
        try {
            remoteService.ping().get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            fail("Even though wait for data returned the pinging immediately afterwards failed");
        }
        communicationService.deactivate();
        remoteService.deactivate();
        remoteService.reinit();

        communicationService.shutdown();
        remoteService.shutdown();


        try {
            remoteService.reinit();
            fail("No exception occurred.");
        } catch (CouldNotPerformException ex) {
            // this should happen
        }
    }

    public static class AbstractControllerServerImpl extends AbstractControllerServer<UnitRegistryData, UnitRegistryData.Builder> {

        public AbstractControllerServerImpl(UnitRegistryData.Builder builder) throws InstantiationException {
            super(builder);
        }

        @Override
        public void registerMethods(RPCServer server) throws CouldNotPerformException {

        }
    }

    public static class AbstractRemoteClientImpl extends AbstractRemoteClient<UnitRegistryData> {
        public AbstractRemoteClientImpl() {
            super(UnitRegistryData.class);
        }
    }
}
