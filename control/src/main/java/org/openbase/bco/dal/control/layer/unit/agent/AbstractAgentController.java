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
 */

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.control.layer.unit.AbstractAuthorizedBaseUnitController;
import org.openbase.bco.dal.control.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.type.domotic.unit.agent.AgentDataType.AgentData.Builder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.agent.AgentDataType;
import org.openbase.type.domotic.unit.agent.AgentDataType.AgentData;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractAgentController extends AbstractAuthorizedBaseUnitController<AgentData, Builder> implements AgentController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    public AbstractAgentController(final Class unitClass) throws InstantiationException {
        super(unitClass, AgentDataType.AgentData.newBuilder());
    }

    @Override
    protected ActionParameter.Builder getActionParameterTemplate(final UnitConfig config) throws InterruptedException, CouldNotPerformException {
        final AgentClass agentClass = Registries.getClassRegistry(true).getAgentClassById(config.getAgentConfig().getAgentClassId());
        return ActionParameter.newBuilder()
                .addAllCategory(agentClass.getCategoryList())
                .setPriority(agentClass.getPriority())
                .setSchedulable(agentClass.getSchedulable())
                .setInterruptible(agentClass.getInterruptible());
    }

    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return getConfig().getAgentConfig().getAutostart();
    }
}
