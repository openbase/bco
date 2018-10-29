package org.openbase.bco.dal.control.layer.unit.agent;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.control.layer.unit.UnitControllerRegistrySynchronizer;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentManager;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentManagerImpl implements AgentManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AgentManagerImpl.class);

    private final AgentControllerFactory factory;
    private final UnitControllerRegistry<AgentController> agentControllerRegistry;
    private final UnitControllerRegistrySynchronizer<AgentController> agentRegistrySynchronizer;

    public AgentManagerImpl() throws InstantiationException {
        try {
            this.factory = AgentControllerFactoryImpl.getInstance();
            this.agentControllerRegistry = new UnitControllerRegistryImpl<>();
            this.agentRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(agentControllerRegistry, Registries.getUnitRegistry().getAgentUnitConfigRemoteRegistry(), factory);
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() {
        // this has to stay, else do not implement VoidInitializable
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        BCOLogin.loginBCOUser();
        agentControllerRegistry.activate();
        agentRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return agentRegistrySynchronizer.isActive() &&
                agentControllerRegistry.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        agentRegistrySynchronizer.deactivate();
        agentControllerRegistry.deactivate();
    }

    @Override
    public void shutdown() {
        agentRegistrySynchronizer.shutdown();
        agentControllerRegistry.shutdown();
    }

    @Override
    public UnitControllerRegistry<AgentController> getAgentControllerRegistry() {
        return agentControllerRegistry;
    }
}
