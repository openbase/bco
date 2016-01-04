package org.dc.bco.coma.dem.core;

import de.citec.dal.DALService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import org.dc.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.dc.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.dc.jul.storage.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.state.ActivationStateType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceManagerLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManagerLauncher.class);


    private final DeviceFactory factory;
    private final Registry<String, Agent> agentRegistry;
    private final AgentRegistryRemote agentRegistryRemote;
    private final ActivatableEntryRegistrySynchronizer<String, Agent, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder> registrySynchronizer;

    public public class DeviceManagerLauncher {
() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            this.factory = AgentFactoryImpl.getInstance();
            this.agentRegistry = new Registry<>();

            agentRegistryRemote = new AgentRegistryRemote();

            this.registrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, Agent, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder>(agentRegistry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

                @Override
                public boolean activationCondition(final AgentConfigType.AgentConfig config) {
                    return config.getActivationState().getValue() == ActivationStateType.ActivationState.State.ACTIVE;
                }
            };

            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            registrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.dc.jul.exception.CouldNotPerformException
     */
    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(DeviceManagerLauncher.class);
        JPService.registerProperty(JPHardwareSimulationMode.class);
        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new DeviceManagerLauncher();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
