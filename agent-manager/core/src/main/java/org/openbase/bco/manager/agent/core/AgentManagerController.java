package org.openbase.bco.manager.agent.core;

/*
 * #%L
 * COMA AgentManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
import java.util.concurrent.TimeUnit;
import org.openbase.bco.manager.agent.lib.AgentController;
import org.openbase.bco.manager.agent.lib.AgentFactory;
import org.openbase.bco.manager.agent.lib.AgentManager;
import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ControllerRegistry;
import org.openbase.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class AgentManagerController implements AgentManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AgentManagerController.class);

    private static AgentManagerController instance;

    private final AgentFactory factory;
    private final ControllerRegistry<String, AgentController> agentRegistry;
    private final AgentRegistryRemote agentRegistryRemote;
    private final EnableableEntryRegistrySynchronizer<String, AgentController, UnitConfig, UnitConfig.Builder> agentRegistrySynchronizer;

    public AgentManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;
            this.factory = AgentFactoryImpl.getInstance();
            this.agentRegistry = new ControllerRegistry<>();
            this.agentRegistryRemote = new AgentRegistryRemote();

            this.agentRegistrySynchronizer = new EnableableEntryRegistrySynchronizer<String, AgentController, UnitConfig, UnitConfig.Builder>(agentRegistry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

                @Override
                public boolean enablingCondition(final UnitConfig config) {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public static AgentManagerController getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(AgentManagerController.class);
        }
        return instance;
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            agentRegistryRemote.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        agentRegistryRemote.activate();
        
        // TODO: pleminoq: let us analyse why this wait For Datta is needed. Without the sychnchronizer sync task is interrupted. And why is this never happening in the unit tests???
        agentRegistryRemote.waitForData();
        
        agentRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return agentRegistryRemote.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        agentRegistrySynchronizer.deactivate();
        agentRegistryRemote.deactivate();
    }

    @Override
    public void shutdown() {
        agentRegistrySynchronizer.shutdown();
        agentRegistryRemote.shutdown();
        instance = null;
    }

    @Override
    public void waitForInit(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        agentRegistryRemote.waitForData(timeout, timeUnit);
    }
}
