package org.dc.bco.manager.agent.core;

import org.dc.bco.manager.agent.lib.AgentController;
import org.dc.bco.manager.agent.lib.AgentFactory;
import org.dc.bco.manager.agent.lib.AgentManager;
import org.dc.bco.registry.agent.remote.AgentRegistryRemote;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.bco.registry.device.lib.provider.DeviceRegistryProvider;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.dc.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.state.ActivationStateType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class AgentManagerController implements DeviceRegistryProvider, AgentManager {

    protected static final Logger logger = LoggerFactory.getLogger(AgentManagerController.class);

    private static AgentManagerController instance;
    private final AgentFactory factory;
    private final RegistryImpl<String, AgentController> agentRegistry;
    private final AgentRegistryRemote agentRegistryRemote;
    private final ActivatableEntryRegistrySynchronizer<String, AgentController, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder> registrySynchronizer;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public AgentManagerController() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;
            this.factory = AgentFactoryImpl.getInstance();
            this.agentRegistry = new RegistryImpl<>();
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.agentRegistryRemote = new AgentRegistryRemote();

            this.registrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, AgentController, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder>(agentRegistry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

                @Override
                public boolean activationCondition(final AgentConfigType.AgentConfig config) {
                    return config.getActivationState().getValue() == ActivationStateType.ActivationState.State.ACTIVE;
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public static AgentManagerController getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(AgentManagerController.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        this.agentRegistryRemote.init();
        this.deviceRegistryRemote.init();
    }

    public void activate() throws CouldNotPerformException, InterruptedException {
        this.deviceRegistryRemote.activate();
        this.agentRegistryRemote.activate();
        this.registrySynchronizer.init();
    }

    @Override
    public DeviceRegistry getDeviceRegistry() throws NotAvailableException {
        return this.deviceRegistryRemote;
    }
}
