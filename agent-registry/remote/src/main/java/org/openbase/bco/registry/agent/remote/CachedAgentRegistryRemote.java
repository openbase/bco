package org.openbase.bco.registry.agent.remote;

/*
 * #%L
 * BCO Registry Agent Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.bco.registry.agent.lib.AgentRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class CachedAgentRegistryRemote {

    private static final Logger logger = LoggerFactory.getLogger(CachedAgentRegistryRemote.class);
    private static AgentRegistryRemote agentRegistryRemote;
    private static boolean shutdown = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                shutdown = true;
                shutdown();
            }
        });
    }

    public static void reinitialize() throws InterruptedException, CouldNotPerformException {
        try {
            getRegistry();
            agentRegistryRemote.requestData().get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not reinitialize " + CachedAgentRegistryRemote.class.getSimpleName() + "!", ex);
        }
    }

    /**
     *
     * @return @throws InterruptedException
     * @throws NotAvailableException
     */
    public synchronized static AgentRegistryRemote getRegistry() throws InterruptedException, NotAvailableException {
        try {
            if (shutdown) {
                throw new InvalidStateException("Remote service is shutting down!");
            }

            if (agentRegistryRemote == null) {
                try {
                    agentRegistryRemote = new AgentRegistryRemote();
                    agentRegistryRemote.init();
                    agentRegistryRemote.activate();
                } catch (CouldNotPerformException ex) {
                    if (agentRegistryRemote != null) {
                        agentRegistryRemote.shutdown();
                        agentRegistryRemote = null;
                    }
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not start cached agent registry remote!", ex), logger);
                }
            }
            return agentRegistryRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("cached agent registry", ex);
        }
    }

    public static void waitForData() throws InterruptedException, CouldNotPerformException {
        if (agentRegistryRemote == null) {
            getRegistry();
        }
        agentRegistryRemote.waitForData();
    }

    public static void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        if (agentRegistryRemote == null) {
            getRegistry();
        }
        agentRegistryRemote.waitForData(timeout, timeUnit);
    }

    public static void shutdown() {
        if (agentRegistryRemote != null) {
            agentRegistryRemote.shutdown();
            agentRegistryRemote = null;
        }
    }
}
