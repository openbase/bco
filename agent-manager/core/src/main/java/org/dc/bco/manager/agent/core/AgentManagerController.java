package org.dc.bco.manager.agent.core;

/*
 * #%L
 * COMA AgentManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.dc.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentConfigType;
import rst.homeautomation.state.EnablingStateType.EnablingState;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class AgentManagerController implements DeviceRegistryProvider, AgentManager {

    protected static final Logger logger = LoggerFactory.getLogger(AgentManagerController.class);

    private static AgentManagerController instance;
    private final AgentFactory factory;
    private final RegistryImpl<String, AgentController> agentRegistry;
    private final AgentRegistryRemote agentRegistryRemote;
    private final EnableableEntryRegistrySynchronizer<String, AgentController, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder> registrySynchronizer;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public AgentManagerController() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;
            this.factory = AgentFactoryImpl.getInstance();
            this.agentRegistry = new RegistryImpl<>();
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.agentRegistryRemote = new AgentRegistryRemote();

            this.registrySynchronizer = new EnableableEntryRegistrySynchronizer<String, AgentController, AgentConfigType.AgentConfig, AgentConfigType.AgentConfig.Builder>(agentRegistry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

                @Override
                public boolean enablingCondition(final AgentConfigType.AgentConfig config) {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
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
        try {
            this.agentRegistryRemote.init();
            this.agentRegistryRemote.activate();
            this.deviceRegistryRemote.init();
            this.deviceRegistryRemote.activate();
            this.agentRegistryRemote.waitForData();
            this.deviceRegistryRemote.waitForData();
            this.registrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        this.agentRegistryRemote.shutdown();
        this.deviceRegistryRemote.shutdown();
        this.deviceRegistryRemote.shutdown();
        instance = null;
    }

    @Override
    public DeviceRegistry getDeviceRegistry() throws NotAvailableException {
        return this.deviceRegistryRemote;
    }
}
