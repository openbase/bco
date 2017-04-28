package org.openbase.bco.dal.lib.layer.service.operation;

/*
 * #%L
 * BCO DAL Library
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
import java.util.UUID;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.provider.PowerStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.calendar.DateTimeType.DateTime;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionReferenceType.ActionReference;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActionStateType.ActionState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface PowerStateOperationService extends OperationService, PowerStateProviderService {

    @RPCMethod
    public Future<Void> setPowerState(final PowerState powerState) throws CouldNotPerformException;

    public default Future<Void> setPowerState(final PowerState powerState, ActionDescription actionDescription) throws CouldNotPerformException {
        ActionDescription.Builder actionDescriptionBuilder = actionDescription.toBuilder();
        ResourceAllocation.Builder resourceAllocation = actionDescriptionBuilder.getResourceAllocationBuilder();
        ServiceStateDescription.Builder serviceStateDescription = actionDescriptionBuilder.getServiceStateDescriptionBuilder();
        ServiceJSonProcessor jSonProcessor = new ServiceJSonProcessor();

        // can be set without parameters
        actionDescriptionBuilder.setId(UUID.randomUUID().toString()); // UUID
        actionDescriptionBuilder.setLabel("UnitLabel[" + powerState.getValue().name() + "]");  // unit label
        actionDescriptionBuilder.setDescription("Mr. Pink changed " + ServiceType.POWER_STATE_SERVICE.name() + " of unit UNITLABEL to " + powerState.getValue().name()); // value to be set
        actionDescriptionBuilder.setResourceAllocation(resourceAllocation);
        actionDescriptionBuilder.setServiceStateDescription(serviceStateDescription);

        //resourceAllocation.setId(UUID.randomUUID().toString()); empty
        resourceAllocation.addResourceIds("UNITSCOPE"); // scope
        resourceAllocation.setDescription(actionDescriptionBuilder.getDescription());

        serviceStateDescription.setServiceAttribute(jSonProcessor.serialize(powerState));
        serviceStateDescription.setServiceAttributeType(jSonProcessor.getServiceAttributeType(ServiceType.PRESENCE_STATE_SERVICE));
        serviceStateDescription.setServiceType(ServiceType.POWER_STATE_SERVICE);
        serviceStateDescription.setUnitId("UNITID"); // id of the unit

        // enums empty or default values at beginning
        actionDescriptionBuilder.setActionState(ActionState.newBuilder().setValue(ActionState.State.INITIALIZED).build());
//        resourceAllocation.setState(ResourceAllocation.State.); // empty
//        resourceAllocation.setSlot(Interval.getDefaultInstance()); // computation in UnitAllocator from startTime and ExecutionTimePeriod

        // given as parameter
        // optional
        resourceAllocation.setPriority(ResourceAllocation.Priority.NORMAL); // default is normal
        actionDescriptionBuilder.setActionChain(0, ActionReference.getDefaultInstance()); // parameter is actionDescription of initiator to build reference and chain
        actionDescriptionBuilder.setExecutionTimePeriod(0); // 0 should be default value but others have to be set
        // like actionAuthority globale parameter, dependent und priority -> lower -> longer
        actionDescriptionBuilder.setExecutionValidity(DateTime.getDefaultInstance()); // default an hour
        resourceAllocation.setPolicy(ResourceAllocation.Policy.FIRST);
        serviceStateDescription.setUnitType(UnitTemplate.UnitType.UNKNOWN); // default unknown

        // required
        resourceAllocation.setInitiator(ResourceAllocation.Initiator.HUMAN); // no default value? like actionAuthority given once?
        actionDescriptionBuilder.setActionAuthority(ActionAuthority.getDefaultInstance()); // every time as a paramter
        return null;
    }
}
