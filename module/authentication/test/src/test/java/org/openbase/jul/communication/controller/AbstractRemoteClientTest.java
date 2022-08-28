package org.openbase.jul.communication.controller;

/*-
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

import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.communication.controller.AbstractControllerServerTest.AbstractControllerServerImpl;
import org.openbase.jul.communication.controller.AbstractControllerServerTest.AbstractRemoteClientImpl;
import org.openbase.jul.communication.iface.RPCServer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.TimeoutException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.type.util.TransactionSynchronizationFuture;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.unit.dal.PowerSwitchDataType.PowerSwitchData;
import org.openbase.type.domotic.unit.dal.PowerSwitchDataType.PowerSwitchData.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openbase.type.domotic.state.AvailabilityStateType.AvailabilityState.State.ONLINE;
import static org.openbase.type.domotic.state.ConnectionStateType.ConnectionState.State.CONNECTED;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AbstractRemoteClientTest extends MqttIntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractRemoteClientTest() {
    }

    /**
     * Test of waitForConnectionState method, of class AbstractRemoteClient.
     *
     * @throws InterruptedException
     * @throws CouldNotPerformException
     */
    @Test
    @Timeout(10)
    public void testWaitForConnectionState() throws InterruptedException, CouldNotPerformException {
        System.out.println("waitForConnectionState");
        AbstractRemoteClient instance = new AbstractRemoteClientImpl();
        instance.init("/test/waitForConnectionState");

        // Test Timeout
        instance.activate();

        try {
            instance.waitForConnectionState(CONNECTED, 10);
            fail("No exception thrown.");
        } catch (TimeoutException ex) {
            // should be thrown...
            assertTrue(true);
        }

        // Test if shutdown is blocked by waitForConnection without timeout
        System.out.println("Test if waitForConnection is interrupted through shutdown!");
        GlobalCachedExecutorService.submit(() -> {
            try {
                System.out.println("Thread is running");
                assertTrue(instance.isActive(), "Instance is not active while waiting");
                System.out.println("Wait for ConnectionState");
                instance.waitForConnectionState(CONNECTED);
            } catch (CouldNotPerformException | InterruptedException ex) {
//                    ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
            }
            return null;
        });

        Thread.sleep(100);

        instance.shutdown();
    }

    @Test
    @Timeout(10)
    public void testDeactivation() throws InterruptedException, CouldNotPerformException {
        System.out.println("testDeactivation");
        final String scope = "/test/deactivation";

        AbstractRemoteClient instance = new AbstractRemoteClientImpl();
        instance.init(scope);
        instance.activate();

        AbstractControllerServerImpl communicationService = new AbstractControllerServerImpl(UnitRegistryData.newBuilder());
        communicationService.init(scope);
        communicationService.activate();
        communicationService.waitForAvailabilityState(ONLINE);

        instance.waitForConnectionState(CONNECTED);
        instance.waitForData();
        System.out.println("shutdown...");
        System.out.println("main thread name: " + Thread.currentThread().getName());
        communicationService.deactivate();
        instance.deactivate();
        communicationService.shutdown();
        instance.shutdown();
    }

    /**
     * Test what happens when one thread calls an asynchronous method while another reinitializes
     * the remote services and requests new data afterwards.
     * This is a simple example for issue https://github.com/openbase/bco.registry/issues/59,
     *
     * @throws Exception
     */
    @Test
    @Timeout(5)
    public void testReinit() throws Exception {
        System.out.println("testReinit");

        final AbstractRemoteClient remoteService = new AbstractRemoteClientImpl();
        remoteService.init("/test/testReinit");
        remoteService.activate();

        boolean[] check = new boolean[1];
        check[0] = false;
        GlobalCachedExecutorService.submit(() -> {
            try {
                remoteService.callMethodAsync("method", Any.class).get();
            } catch (InterruptedException | ExecutionException ex) {
                // is expected since reinit should kill the method call
                check[0] = true;
            }
        });

        Thread.sleep(100);

        remoteService.reinit();
        try {
            remoteService.requestData().get(100, TimeUnit.MILLISECONDS);
        } catch (CancellationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Requesting data has been cancelled", ex), logger);
        } catch (java.util.concurrent.TimeoutException ex) {
            // is expected here since no server is started
        }

        assertTrue(check[0], "call was not canceled!");

        remoteService.shutdown();
    }

    private boolean prioritizedObservableFinished = false;

    /**
     * Test for the synchronization using transaction ids. This tests verifies if the {@link TransactionSynchronizationFuture}
     * can only return after the internal prioritized observable of the remote service has finished its notification.
     * <p>
     * This is needed e.g. for registry remotes because they synchronize their internal remote registries using this
     * observable. If it is not finished when the future returns following calls can fail.
     * See issue: <a href="https://github.com/openbase/bco.registry/issues/98">https://github.com/openbase/bco.registry/issues/98</a>
     *
     * @throws Exception if an error occurs.
     */
    @Test
    @Timeout(5)
    public void testTransactionSynchronization() throws Exception {
        final String scope = "/test/transaction/sync";

        final TransactionControllerServer communicationService = new TransactionControllerServer();
        communicationService.init(scope);
        communicationService.activate();

        final TransactionRemoteClient remoteService = new TransactionRemoteClient();
        remoteService.init(scope);
        remoteService.activate();
        remoteService.waitForData();

        long transactionId = remoteService.getTransactionId();
        remoteService.getInternalPrioritizedDataObservable().addObserver((source, data) -> {
            Thread.sleep(100);
            prioritizedObservableFinished = true;
        });
        remoteService.performTransaction().get();
        assertTrue(remoteService.getTransactionId() > transactionId, "Transaction id did not increase after performTransaction call");
        assertTrue(prioritizedObservableFinished, "Prioritized observable is not finished but sync future already returned");

        remoteService.shutdown();
        communicationService.shutdown();
    }

    public static class TransactionControllerServer extends AbstractControllerServer<PowerSwitchData, Builder> {

        /**
         * Create a communication service.
         *
         * @throws InstantiationException if the creation fails
         */
        public TransactionControllerServer() throws CouldNotPerformException {
            super(PowerSwitchData.newBuilder());
        }

        @Override
        public void registerMethods(RPCServer server) throws CouldNotPerformException {
            server.registerMethods((Class) getClass(), this);
        }

        @RPCMethod
        public TransactionValue performTransaction() throws CouldNotPerformException, InterruptedException {
            // update transaction
            updateTransactionId();
            // change data builder to trigger notification
            try (ClosableDataBuilder<Builder> dataBuilder = getDataBuilderInterruptible(this)) {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(State.ON);
            }
            // return transaction value
            return TransactionValue.newBuilder().setTransactionId(getTransactionId()).build();
        }
    }

    private static class TransactionRemoteClient extends AbstractRemoteClient<PowerSwitchData> {

        public TransactionRemoteClient() {
            super(PowerSwitchData.class);
        }

        public Future<TransactionValue> performTransaction() {
            return new TransactionSynchronizationFuture<>(
                FutureProcessor.postProcess(
                    (input, timeout, timeUnit) -> input.getResponse(),
                    this.callMethodAsync("performTransaction", TransactionValue.class)
                ),
                this
            );
        }
    }
}
