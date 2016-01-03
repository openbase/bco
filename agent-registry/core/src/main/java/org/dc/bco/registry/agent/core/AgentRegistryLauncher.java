/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.agent.core;

import org.dc.bco.registry.agent.lib.jp.JPAgentClassDatabaseDirectory;
import org.dc.bco.registry.agent.lib.jp.JPAgentConfigDatabaseDirectory;
import org.dc.bco.registry.agent.lib.jp.JPAgentRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPDebugMode;
import org.dc.jps.preset.JPReadOnly;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.dc.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.dc.jul.storage.registry.jp.JPInitializeDB;
import org.dc.jps.preset.JPForce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
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
        JPService.registerProperty(JPAgentConfigDatabaseDirectory.class);
        JPService.registerProperty(JPAgentClassDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        AgentRegistryLauncher agentManager;
        try {
            agentManager = new AgentRegistryLauncher();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!agentManager.getAgentRegistry().getAgentConfigRegistry().isConsistent()) {
            exceptionStack = MultiException.push(agentManager, new VerificationFailedException("AgentConfigRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(APP_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
