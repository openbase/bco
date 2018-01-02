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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.pattern.trigger.TriggerPool;
import org.openbase.bco.manager.agent.lib.AgentController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.calendar.DateTimeType;
import rst.communicationpatterns.ResourceAllocationType;
import rst.domotic.action.ActionAuthorityType;
import rst.domotic.action.ActionDescriptionType;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.MultiResourceAllocationStrategyType;
import rst.domotic.service.ServiceStateDescriptionType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EmphasisStateType.EmphasisState;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.agent.AgentDataType;
import rst.domotic.unit.agent.AgentDataType.AgentData;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
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
    public Future<ActionFuture> setEmphasisState(EmphasisState emphasisState) throws CouldNotPerformException {
        logger.debug("Apply emphasisState Update[" + emphasisState + "] for " + this + ".");

        try (ClosableDataBuilder<AgentData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setEmphasisState(emphasisState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply emphasisState Update[" + emphasisState + "] for " + this + "!", ex);
        }
        CompletableFuture<ActionFuture> completableFuture = new CompletableFuture<>();
        completableFuture.complete(ActionFuture.getDefaultInstance());
        return completableFuture;
    }

    @Override
    public EmphasisState getEmphasisState() throws NotAvailableException {
        return getData().getEmphasisState();
    }

    protected ActionDescriptionType.ActionDescription.Builder getNewActionDescription(ActionAuthorityType.ActionAuthority actionAuthority,
            ResourceAllocationType.ResourceAllocation.Initiator initiator,
            long executionTimePeriod,
            ResourceAllocationType.ResourceAllocation.Policy policy,
            ResourceAllocationType.ResourceAllocation.Priority priority,
            UnitRemote unitRemote,
            Object serviceAttribute,
            UnitTemplateType.UnitTemplate.UnitType unitType,
            ServiceTemplateType.ServiceTemplate.ServiceType serviceType,
            MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.Strategy multiResourceAllocationStrategy) throws CouldNotPerformException {

        ActionDescriptionType.ActionDescription.Builder actionDescriptionBuilder = ActionDescriptionProcessor.getActionDescription(actionAuthority, initiator);
        actionDescriptionBuilder.setExecutionTimePeriod(executionTimePeriod);
        if (executionTimePeriod != 0) {
            DateTimeType.DateTime dateTime = DateTimeType.DateTime.newBuilder().setDateTimeType(DateTimeType.DateTime.Type.FLOATING).setMillisecondsSinceEpoch(System.currentTimeMillis() + executionTimePeriod).build();
            actionDescriptionBuilder.setExecutionValidity(dateTime);
        }
        actionDescriptionBuilder.setMultiResourceAllocationStrategy(MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy.newBuilder().setStrategy(multiResourceAllocationStrategy).build());

        ResourceAllocationType.ResourceAllocation.Builder resourceAllocation = actionDescriptionBuilder.getResourceAllocationBuilder();
        resourceAllocation.setPolicy(policy);
        resourceAllocation.setPriority(priority);
        resourceAllocation.addResourceIds(ScopeGenerator.generateStringRep(unitRemote.getScope()));

        Service.upateActionDescription(actionDescriptionBuilder, serviceAttribute, serviceType);
        ServiceStateDescriptionType.ServiceStateDescription.Builder serviceStateDescription = actionDescriptionBuilder.getServiceStateDescriptionBuilder();
        serviceStateDescription.setUnitId(unitRemote.getId().toString());
        serviceStateDescription.setUnitType(unitType);

        actionDescriptionBuilder.setDescription(actionDescriptionBuilder.getDescription().replace(ActionDescriptionProcessor.LABEL_KEY, unitRemote.getLabel()));
        actionDescriptionBuilder.setLabel(actionDescriptionBuilder.getLabel().replace(ActionDescriptionProcessor.LABEL_KEY, unitRemote.getLabel()));

        ActionDescriptionProcessor.updateResourceAllocationSlot(actionDescriptionBuilder);
        ActionDescriptionProcessor.updateResourceAllocationId(actionDescriptionBuilder);

        return actionDescriptionBuilder;
    }
}
