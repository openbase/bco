package org.openbase.bco.manager.agent.core;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.manager.agent.lib.AgentController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.AbstractExecutableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.agent.AgentDataType;
import rst.domotic.unit.agent.AgentDataType.AgentData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public abstract class AbstractAgent extends AbstractExecutableController<AgentDataType.AgentData, AgentDataType.AgentData.Builder, UnitConfig> implements AgentController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    public AbstractAgent() throws InstantiationException {
        super(AgentDataType.AgentData.newBuilder());
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(org.openbase.bco.dal.lib.layer.unit.agent.Agent.class, this, server);
    }
    
    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return getConfig().getAgentConfig().getAutostart();
    }   
}
