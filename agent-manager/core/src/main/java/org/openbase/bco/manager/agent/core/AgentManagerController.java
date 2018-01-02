package org.openbase.bco.manager.agent.core;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.manager.agent.lib.AgentController;
import org.openbase.bco.manager.agent.lib.AgentFactory;
import org.openbase.bco.manager.agent.lib.AgentManager;
import org.openbase.bco.registry.login.SystemLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.openbase.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class AgentManagerController implements AgentManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AgentManagerController.class);

    private final AgentFactory factory;
    private final ControllerRegistryImpl<String, AgentController> agentRegistry;
    private final EnableableEntryRegistrySynchronizer<String, AgentController, UnitConfig, UnitConfig.Builder> agentRegistrySynchronizer;

    public AgentManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.factory = AgentFactoryImpl.getInstance();
            this.agentRegistry = new ControllerRegistryImpl<>();

            this.agentRegistrySynchronizer = new EnableableEntryRegistrySynchronizer<String, AgentController, UnitConfig, UnitConfig.Builder>(agentRegistry, Registries.getAgentRegistry().getAgentConfigRemoteRegistry(), Registries.getAgentRegistry(), factory) {

                @Override
                public boolean enablingCondition(final UnitConfig config) {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        // TODO: pleminoq: let us analyse why this wait For Datta is needed. Without the sychnchronizer sync task is interrupted. And why is this never happening in the unit tests???
        Registries.waitForData();

        SystemLogin.loginBCOUser();

        agentRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return agentRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        agentRegistrySynchronizer.deactivate();
    }

    @Override
    public void shutdown() {
        agentRegistrySynchronizer.shutdown();
    }

    @Override
    public void waitForInit(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        Registries.getAgentRegistry().waitForData(timeout, timeUnit);
    }
}
