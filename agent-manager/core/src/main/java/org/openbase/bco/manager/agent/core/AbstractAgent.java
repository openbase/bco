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
 */
import org.openbase.bco.manager.agent.lib.Agent;
import org.openbase.bco.manager.agent.lib.AgentController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.AbstractExecutableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentDataType;
import rst.homeautomation.control.agent.AgentDataType.AgentData;
import rst.homeautomation.state.ActivationStateType.ActivationState;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;

/**
 *
 * * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine
 *
 */
public abstract class AbstractAgent extends AbstractExecutableController<AgentDataType.AgentData, AgentDataType.AgentData.Builder, AgentConfig> implements AgentController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    public AbstractAgent(boolean autostart) throws InstantiationException {
        super(AgentDataType.AgentData.newBuilder(), autostart);
    }

    @Override
    public void init(final AgentConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        logger.info("Initializing " + getClass().getSimpleName() + "[" + config.getId() + "] on scope [" + config.getScope() + "]");
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Agent.class, this, server);
    }
}
