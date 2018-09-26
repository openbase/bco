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
 */

import org.openbase.bco.dal.control.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.manager.agent.lib.AgentController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.trigger.TriggerPool;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EmphasisStateType.EmphasisState;
import rst.domotic.unit.agent.AgentDataType;
import rst.domotic.unit.agent.AgentDataType.AgentData;

import java.util.concurrent.Future;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractAgentController extends AbstractExecutableBaseUnitController<AgentData, AgentData.Builder> implements AgentController {

    protected TriggerPool agentTriggerHolder;

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    public AbstractAgentController(final Class unitClass) throws InstantiationException {
        super(unitClass, AgentDataType.AgentData.newBuilder());
        agentTriggerHolder = new TriggerPool();
    }

    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return getConfig().getAgentConfig().getAutostart();
    }

    @Override
    public Future<ActionFuture> setEmphasisState(EmphasisState state) throws CouldNotPerformException {
        return applyUnauthorizedAction(state, EMPHASIS_STATE_SERVICE);
    }
}
