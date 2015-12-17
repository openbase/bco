/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent;

import de.citec.agm.remote.AgentRegistryRemote;
import de.citec.dal.remote.DALRemote;
import de.citec.dal.remote.jp.JPRemoteMethod;
import de.citec.dal.remote.jp.JPRemoteMethodParameters;
import de.citec.dal.remote.jp.JPRemoteService;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPDebugMode;
import de.citec.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.rsb.scope.jp.JPScope;
import de.citec.jul.storage.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author mpohling
 */
public class AgentScheduler {

    protected static final Logger logger = LoggerFactory.getLogger(AgentScheduler.class);

    public static final String APP_NAME = AgentScheduler.class.getSimpleName();
    
    private final AgentFactory factory;
    private final Registry<String, Agent> agentRegistry;
    private final AgentRegistryRemote agentRegistryRemote;
    private final ActivatableEntryRegistrySynchronizer<String, Agent, AgentConfig, AgentConfig.Builder> registrySynchronizer;

    public AgentScheduler() throws InstantiationException, InterruptedException {
        logger.info("Starting agent scheduler");
        try {
            this.factory = AgentFactoryImpl.getInstance();
            this.agentRegistry = new Registry<>();

            agentRegistryRemote = new AgentRegistryRemote();

            this.registrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, Agent, AgentConfig, AgentConfig.Builder>(agentRegistry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

                @Override
                public boolean activationCondition(final AgentConfig config) {
                    return config.getActivationState().getValue() == ActivationState.State.ACTIVE;
                }
            };

            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            registrySynchronizer.init();
            logger.info("waiting for agents...");

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPScope.class);
        JPService.registerProperty(JPRemoteService.class);
        JPService.registerProperty(JPRemoteMethod.class);
        JPService.registerProperty(JPRemoteMethodParameters.class);

        JPService.parseAndExitOnError(args);

        try {
            new AgentScheduler();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
