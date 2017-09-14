package org.openbase.bco.dal.remote.unit.agent;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.unit.agent.Agent;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EmphasisStateType.EmphasisState;
import rst.domotic.unit.agent.AgentDataType.AgentData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AgentRemote extends AbstractUnitRemote<AgentData> implements Agent {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(EmphasisState.getDefaultInstance()));
    }

    public AgentRemote() {
        super(AgentData.class);
    }

    @Override
    public Future<ActionFuture> setActivationState(final ActivationState activationState) throws CouldNotPerformException {
        logger.info("Calling remote setActivationState to [" + activationState.getValue() + "] for agent");
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        try {
            return applyAction(updateActionDescription(actionDescription, activationState).build());
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Interrupted while setting activationState.", ex);
        }
    }

    @Override
    public ActivationState getActivationState() throws NotAvailableException {
        return getData().getActivationState();
    }

    @Override
    public Future<ActionFuture> setEmphasisState(EmphasisState emphasisState) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        try {
            return applyAction(updateActionDescription(actionDescription, emphasisState).build());
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Interrupted while setting emphasisState.", ex);
        }
    }

    @Override
    public EmphasisState getEmphasisState() throws NotAvailableException {
        return getData().getEmphasisState();
    }
}
