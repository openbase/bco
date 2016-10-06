package org.openbase.bco.registry.agent.core;

/*
 * #%L
 * REM AgentRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.registry.agent.lib.jp.JPAgentClassDatabaseDirectory;
import org.openbase.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.openbase.jul.storage.registry.jp.JPInitializeDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AgentRegistryLauncher {

    private static final Logger logger = LoggerFactory.getLogger(AgentRegistryLauncher.class);

    public static final String APP_NAME = AgentRegistryLauncher.class.getSimpleName();

    private final AgentRegistryController agentRegistry;

    public AgentRegistryLauncher() throws InitializationException, InterruptedException {
        try {
            this.agentRegistry = new AgentRegistryController();
            this.agentRegistry.init();
            this.agentRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (agentRegistry != null) {
            agentRegistry.shutdown();
        }
    }

    public AgentRegistryController getAgentRegistry() {
        return agentRegistry;
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPAgentRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPAgentClassDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        AgentRegistryLauncher agentRegistry;
        try {
            agentRegistry = new AgentRegistryLauncher();
        } catch (InitializationException ex) {
            ExceptionPrinter.printHistoryAndExit(JPService.getApplicationName() + " crashed during startup phase!", ex, logger);
            return;
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!agentRegistry.getAgentRegistry().getAgentClassRegistry().isConsistent()) {
            exceptionStack = MultiException.push(agentRegistry, new VerificationFailedException("AgentClassRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger);
            return;
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
