package org.openbase.bco.manager.agent.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
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
 */
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.manager.agent.binding.openhab.transform.ActivationStateTransformer;
import org.openbase.bco.dal.remote.unit.agent.AgentRemote;
import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.openbase.jul.storage.registry.RegistrySynchronizer;
import rst.domotic.binding.openhab.OpenhabCommandType.OpenhabCommand;
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

    private final AgentRegistryRemote agentRegistryRemote;
    private final AgentRemoteFactoryImpl factory;
    private final RegistrySynchronizer<String, AgentRemote, UnitConfig, UnitConfig.Builder> registrySynchronizer;
    private final RegistryImpl<String, AgentRemote> registry;
    private final boolean hardwareSimulationMode;
    private boolean active;

    public AgentBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        agentRegistryRemote = new AgentRegistryRemote();
        registry = new RegistryImpl<>();
        factory = new AgentRemoteFactoryImpl();
        hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();

        this.registrySynchronizer = new RegistrySynchronizer<String, AgentRemote, UnitConfig, UnitConfig.Builder>(registry, agentRegistryRemote.getAgentConfigRemoteRegistry(), factory) {

            @Override
            public boolean verifyConfig(final UnitConfig config) throws VerificationFailedException {
                return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
            }
        };

    }

    private String getIdFromOpenHABItem(OpenhabCommand command) {
        return command.getItemBindingConfig().split(":")[1];
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        init(AGENT_MANAGER_ITEM_FILTER, new AbstractOpenHABRemote(hardwareSimulationMode) {

            @Override
            public void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException {
                logger.debug("Ignore update for agent manager openhab binding.");
            }

            @Override
            public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
                try {
                    if (!command.hasOnOff() || !command.getOnOff().hasState()) {
                        throw new CouldNotPerformException("Command does not have an onOff value required for agents");
                    }
                    logger.debug("Received command for agent [" + command.getItem() + "] from openhab");
                    registry.get(getIdFromOpenHABItem(command)).setActivationState(ActivationStateTransformer.transform(command.getOnOff().getState()));
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + command.getOnOff() + "]!", ex);
                }
            }

        });
    }

    @Override
    public void init(String itemFilter, OpenHABRemote openHABRemote) throws InitializationException, InterruptedException {
        super.init(itemFilter, openHABRemote);
        try {
            factory.init(openHABRemote);
            agentRegistryRemote.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        agentRegistryRemote.activate();
        registrySynchronizer.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        agentRegistryRemote.deactivate();
        registrySynchronizer.deactivate();
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
