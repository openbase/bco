package org.openbase.bco.dal.control.layer.unit.agent;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.control.layer.unit.AbstractAuthorizedBaseUnitController;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.type.domotic.unit.agent.AgentDataType.AgentData.Builder;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.agent.AgentDataType;
import org.openbase.type.domotic.unit.agent.AgentDataType.AgentData;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractAgentController extends AbstractAuthorizedBaseUnitController<AgentData, Builder> implements AgentController {

    public AbstractAgentController() throws InstantiationException {
        super(AgentDataType.AgentData.newBuilder());
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
