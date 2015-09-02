/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.agm.core;

import de.citec.agm.core.registry.AgentRegistryService;
import de.citec.jp.JPAgentDatabaseDirectory;
import de.citec.jp.JPAgentClassDatabaseDirectory;
import de.citec.jp.JPAgentConfigDatabaseDirectory;
import de.citec.jp.JPAgentRegistryScope;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.storage.registry.jp.JPGitRegistryPlugin;
import de.citec.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class AgentManager {

    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);

    public static final String APP_NAME = AgentManager.class.getSimpleName();

    private final AgentRegistryService agentRegistry;

    public AgentManager() throws InitializationException, InterruptedException {
        try {
            this.agentRegistry = new AgentRegistryService();
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

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPAgentRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPAgentDatabaseDirectory.class);
        JPService.registerProperty(JPAgentConfigDatabaseDirectory.class);
        JPService.registerProperty(JPAgentClassDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        try {
            new AgentManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
