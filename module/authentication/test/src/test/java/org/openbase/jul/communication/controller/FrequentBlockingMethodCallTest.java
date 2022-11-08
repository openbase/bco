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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openbase.jps.core.JPService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.communication.iface.RPCServer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FrequentBlockingMethodCallTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrequentBlockingMethodCallTest.class);

    /**
     * Deactivated because its a system test which does not really perform any unit tests but tests the capacity of
     * the executor service by repeatedly calling blocking rpc calls.
     * @throws Exception
     */
    @Test
    @Disabled
    public void testFrequentBlockingMethodCall() throws Exception {
        JPService.setupJUnitTestMode();
        final String scope = "/test/blocking";

        AbstractBlockingControllerServerImpl communicationService = new AbstractBlockingControllerServerImpl(UnitRegistryData.newBuilder());
        communicationService.init(scope);
        communicationService.activate();

        BlockingAbstractRemoteClientImpl remoteService = new BlockingAbstractRemoteClientImpl();
        remoteService.init(scope);
        remoteService.activate();

        remoteService.waitForData(1, TimeUnit.SECONDS);

        while (GlobalCachedExecutorService.getInstance().getExecutorService().getActiveCount() < 1960) {
            remoteService.blockForever();

            LOGGER.info("Active count: " + GlobalCachedExecutorService.getInstance().getExecutorService().getActiveCount());
            Thread.sleep(10);
        }

        Thread.sleep(TimeUnit.MINUTES.toMillis(2));

        if (!remoteService.getFutures().get(0).isDone()) {
            LOGGER.error("Future from first task should be done");
        } else {
            try {
                remoteService.getFutures().get(0).get();
            } catch (ExecutionException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            }
        }
    }


    public interface Blocked {

        @RPCMethod
        Void blockForever();
    }

    public class AbstractBlockingControllerServerImpl extends AbstractControllerServer<UnitRegistryData, Builder> implements Blocked {

        public AbstractBlockingControllerServerImpl(Builder builder) throws InstantiationException {
            super(builder);
        }

        @Override
        public void registerMethods(RPCServer server) throws CouldNotPerformException {
            server.registerMethods(Blocked.class, this);
        }

        @Override
        public Void blockForever() {
            LOGGER.info("BlockForever called");
            Integer count = 0;
            while (!Thread.currentThread().isInterrupted()) {
                count = count++;
            }
            LOGGER.info("Quit because thread has been interrupted[" + Thread.currentThread().isInterrupted() + "]");
            return null;
        }
    }

    public class BlockingAbstractRemoteClientImpl extends AbstractRemoteClient<UnitRegistryData> implements Blocked {

        private final List<Future> futureList = new ArrayList<>();

        public BlockingAbstractRemoteClientImpl() {
            super(UnitRegistryData.class);
        }

        @Override
        public Void blockForever() {
             futureList.add(this.callMethodAsync("blockForever", Void.class));
            return null;
        }

        public List<Future> getFutures() {
            return futureList;
        }
    }
}
