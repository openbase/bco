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
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.communication.config.CommunicatorConfig;
import org.openbase.jul.communication.data.RPCResponse;
import org.openbase.jul.communication.iface.CommunicatorFactory;
import org.openbase.jul.communication.iface.RPCClient;
import org.openbase.jul.communication.iface.RPCServer;
import org.openbase.jul.communication.mqtt.CommunicatorFactoryImpl;
import org.openbase.jul.communication.mqtt.DefaultCommunicatorConfig;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.iface.Requestable;
import org.openbase.jul.schedule.WatchDog;
import org.openbase.type.communication.ScopeType.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class FutureCancelTest extends MqttIntegrationTest implements Requestable<Object> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public FutureCancelTest() {
    }

    private boolean run = true;

    @RPCMethod
    @Override
    public Integer requestStatus() throws CouldNotPerformException {
        System.out.println("RequestStatus");
        try {
            while (run) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Interrupted");
                    Thread.currentThread().interrupt();
                }
                System.out.println("Sleeping...");
                Thread.sleep(200);
            }
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
        } catch (CancellationException ex) {
            System.out.println("Cancelled");
        } catch (Exception ex) {
            System.out.println("Other" + ex);
        } catch (Throwable ex) {
            System.out.println("Test" + ex);
        }

        return 0;
    }

    /**
     * This test shows that the method executed by the local server does not
     * get interrupted through canceling the future.
     *
     * @throws Exception
     */
    @Test
    public void testFutureCancellation() throws Exception {
        System.out.println("TestFutureCancellation");

        final CommunicatorFactory factory = CommunicatorFactoryImpl.Companion.getInstance();
        final CommunicatorConfig defaultCommunicatorConfig = DefaultCommunicatorConfig.Companion.getInstance();

        Scope scope = ScopeProcessor.generateScope("/test/futureCancel");

        RPCServer server = factory.createRPCServer(scope, defaultCommunicatorConfig);
        RPCClient client = factory.createRPCClient(scope, defaultCommunicatorConfig);

        WatchDog serverWatchDog = new WatchDog(server, "PRCServer");
        WatchDog clientWatchDog = new WatchDog(client, "RPCClient");

        // register rpc methods.
        server.registerMethods((Class) getClass(), this);

        serverWatchDog.activate();
        serverWatchDog.waitForServiceActivation();

        clientWatchDog.activate();
        clientWatchDog.waitForServiceActivation();

        Future<RPCResponse<Any>> future = client.callMethod("requestStatus", Any.class);
        try {
            future.get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            System.out.println("Future cancelled: " + future.cancel(true));
            Thread.sleep(1000);
        }

        serverWatchDog.shutdown();
        clientWatchDog.shutdown();

        run = false;
    }
}
