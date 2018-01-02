package org.openbase.bco.manager.agent.binding.openhab;

/*
 * #%L
 * BCO Manager Agent Binding OpenHAB
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
 */
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.openbase.jul.storage.registry.RegistrySynchronizer;
import org.openbase.jul.storage.registry.RemoteControllerRegistryImpl;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class AgentBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String AGENT_MANAGER_ITEM_FILTER = "bco.manager.agent";

    private final AgentRemoteFactoryImpl factory;
    private final RegistrySynchronizer<String, AgentRemote, UnitConfig, UnitConfig.Builder> registrySynchronizer;
    private final RemoteControllerRegistryImpl<String, AgentRemote> registry;
    private final boolean hardwareSimulationMode;
    private boolean active;

    public AgentBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        try {
            registry = new RemoteControllerRegistryImpl<>();
            factory = new AgentRemoteFactoryImpl();
            hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();

            this.registrySynchronizer = new RegistrySynchronizer<String, AgentRemote, UnitConfig, UnitConfig.Builder>(registry, Registries.getAgentRegistry().getAgentConfigRemoteRegistry(), Registries.getAgentRegistry(), factory) {

                @Override
                public boolean verifyConfig(final UnitConfig config) throws VerificationFailedException {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };
        } catch (final CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        super.init(AGENT_MANAGER_ITEM_FILTER, new AgentBindingOpenHABRemote(hardwareSimulationMode, registry));
        try {
            Registries.getAgentRegistry().waitForData();
            factory.init(openHABRemote);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        registrySynchronizer.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        registrySynchronizer.deactivate();
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
