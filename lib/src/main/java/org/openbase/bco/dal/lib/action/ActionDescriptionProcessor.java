package org.openbase.bco.dal.lib.action;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.annotation.Experimental;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.TimestampJavaTimeTransform;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.processing.StringProcessor;
import rst.calendar.DateTimeType.DateTime;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation.Initiator;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionAuthorityType.ActionAuthority.Authority;
import rst.domotic.action.ActionAuthorityType.ActionAuthority.Builder;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionDescriptionType.ActionDescriptionOrBuilder;
import rst.domotic.action.ActionParameterType.ActionParameter;
import rst.domotic.action.ActionReferenceType.ActionReference;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActionStateType.ActionState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.timing.IntervalType.Interval;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActionDescriptionProcessor {

    public static final String TOKEN_SEPARATOR = "#";

    public static final String AUTHORITY_KEY = "$AUTHORITY";
    public static final String SERVICE_TYPE_KEY = "$SERVICE_TYPE";
    public static final String LABEL_KEY = "$LABEL";
    public static final String SERVICE_ATTRIBUTE_KEY = "SERVICE_ATTRIBUTE";
    public static final String GENERIC_ACTION_LABEL = LABEL_KEY + "[" + SERVICE_ATTRIBUTE_KEY + "]";
    public static final String GENERIC_ACTION_DESCRIPTION = AUTHORITY_KEY + " changed " + SERVICE_TYPE_KEY + " of unit " + LABEL_KEY + " to " + SERVICE_ATTRIBUTE_KEY;

    public static long MIN_ALLOCATION_TIME_MILLI = 10000;

    /**
     * Get an ActionDescription which only misses unit and service information.
     * Fields which are still missing after:
     * <ul>
     * <li>ActionDescription.Label</li>
     * <li>ActionDescription.Description</li>
     * <li>ActionDescription.ResourceAllocation.ResourceId</li>
     * <li>ActionDescription.ResourceAllocation.Description</li>
     * <li>ActionDescription.ResourceAllocation.UnitId</li>
     * <li>ActionDescription.ResourceAllocation.ServiceType</li>
     * <li>ActionDescription.ResourceAllocation.ServiceAttributeType</li>
     * <li>ActionDescription.ServiceStateDescription.ServiceAttribute</li>
     * </ul>
     *
     * @param actionParameter type which contains several parameters which are updated in the actionDescription
     * @param actionAuthority the actionAuthority for the actionDescription
     * @param initiator       the initiator type for the resourceAllocation in the actionDescription
     * @return an ActionDescription that only misses unit and service information
     */
    public static ActionDescription.Builder getActionDescription(final ActionParameter actionParameter, final ActionAuthority actionAuthority, final ResourceAllocation.Initiator initiator) {
        ActionDescription.Builder actionDescription = ActionDescription.newBuilder();
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();

        // initialize values which are true for every ActionDescription
        actionDescription.setId(UUID.randomUUID().toString());
        actionDescription.setActionState(ActionState.newBuilder().setValue(ActionState.State.INITIALIZED).build());

        LabelProcessor.addLabel(actionDescription.getLabelBuilder(), Locale.ENGLISH, GENERIC_ACTION_LABEL);
        actionDescription.setDescription(GENERIC_ACTION_DESCRIPTION);

        // initialize other required fields from ResourceAllocation
        resourceAllocation.setId(actionDescription.getId());
        resourceAllocation.setSlot(Interval.getDefaultInstance());
        resourceAllocation.setState(ResourceAllocation.State.REQUESTED);

        // add Authority and ResourceAllocation.Initiator
        actionDescription.setActionAuthority(actionAuthority);
        resourceAllocation.setInitiator(initiator);

        // add values from ActionParameter
        actionDescription.setExecutionTimePeriod(actionParameter.getExecutionTimePeriod());
        actionDescription.setExecutionValidity(actionParameter.getExecutionValidity());
        if (actionDescription.getExecutionTimePeriod() != 0 && actionParameter.getPolicy() != ResourceAllocation.Policy.PRESERVE) {
            resourceAllocation.setPolicy(ResourceAllocation.Policy.PRESERVE);
        } else {
            resourceAllocation.setPolicy(actionParameter.getPolicy());
        }
        resourceAllocation.setPriority(actionParameter.getPriority());
        serviceStateDescription.setUnitType(actionParameter.getUnitType());
        // if an initiator action is defined in ActionParameter the actionChain is updated
        if (actionParameter.hasInitiator()) {
            List<ActionReference> actionReferenceList = actionParameter.getInitiator().getActionChainList();
            ActionReference.Builder actionReference = ActionReference.newBuilder();
            actionReference.setActionId(actionParameter.getInitiator().getId());
            actionReference.setAuthority(actionParameter.getInitiator().getActionAuthority());
            actionReference.setServiceStateDescription(actionParameter.getInitiator().getServiceStateDescription());
            actionReferenceList.add(actionReference.build());
            actionDescription.addAllActionChain(actionReferenceList);
        }

        return actionDescription;
    }

    /**
     * Get an ActionDescription which only misses unit and service information.
     * Is created with default ActionParameter.
     * Fields which are still missing after:
     * <ul>
     * <li>ActionDescription.Label</li>
     * <li>ActionDescription.Description</li>
     * <li>ActionDescription.ResourceAllocation.ResourceId</li>
     * <li>ActionDescription.ResourceAllocation.Description</li>
     * <li>ActionDescription.ResourceAllocation.UnitId</li>
     * <li>ActionDescription.ResourceAllocation.ServiceType</li>
     * <li>ActionDescription.ResourceAllocation.ServiceAttributeType</li>
     * <li>ActionDescription.ServiceStateDescription.ServiceAttribute</li>
     * </ul>
     *
     * @param actionAuthority the actionAuthority for the actionDescription
     * @param initiator       the initiator type for the resourceAllocation in the actionDescription
     * @return
     */
    public static ActionDescription.Builder getActionDescription(final ActionAuthority actionAuthority, final ResourceAllocation.Initiator initiator) {
        return getActionDescription(getDefaultActionParameter(), actionAuthority, initiator);
    }

    /**
     * Get default ActionParameter. These are:
     * <ul>
     * <li>Empty initiator, which means that the action has not been triggered by another action</li>
     * <li>Priority = NORMAL</li>
     * <li>ExecutionTimePeriod = 0</li>
     * <li>ExecutionValidityTime = an hour after creation of the ActionParameter type</li>
     * <li>Policy = FIRST</li>
     * <li>UnitType = UNKNOWN</li>
     * </ul>
     *
     * @return an ActionParameter type with the described values
     */
    public static ActionParameter getDefaultActionParameter() {
        ActionParameter.Builder actionParameter = ActionParameter.newBuilder();

        //actionParameter.setInitiator();
        actionParameter.setPriority(ResourceAllocation.Priority.NORMAL);

        actionParameter.setExecutionTimePeriod(0);

        long anHourFromNow = System.currentTimeMillis() + 60 * 60 * 1000;
        DateTime dateTime = DateTime.newBuilder().setDateTimeType(DateTime.Type.FLOATING).setMillisecondsSinceEpoch(anHourFromNow).build();
        actionParameter.setExecutionValidity(dateTime);

        actionParameter.setPolicy(ResourceAllocation.Policy.FIRST);

        actionParameter.setUnitType(UnitTemplate.UnitType.UNKNOWN);

        return actionParameter.build();
    }

    /**
     * Create an interval which start now and ends after the maximum of MIN_ALLCOCATION_TIME_MILLI
     * and the executionTimePeriod.
     * Updates the executionTimePeriod of the given actionDescription.
     *
     * @param actionDescription actionDescription
     * @return an Interval generated as described above
     */
    public static Interval getAllocationInterval(final ActionDescription.Builder actionDescription) {
        Interval.Builder interval = Interval.newBuilder();

        actionDescription.setExecutionTimePeriod(Math.min(actionDescription.getExecutionTimePeriod(), actionDescription.getExecutionValidity().getMillisecondsSinceEpoch() - System.currentTimeMillis()));

        interval.setBegin(TimestampProcessor.getCurrentTimestamp());
        interval.setEnd(TimestampJavaTimeTransform.transform(System.currentTimeMillis() + Math.max(MIN_ALLOCATION_TIME_MILLI, actionDescription.getExecutionTimePeriod())));

        return interval.build();
    }

    /**
     * Update the slot of the ResourceAllocation based on the current time and the
     * values of the ActionDescription.
     * To generate the slot the method {@link #getAllocationInterval(ActionDescription.Builder) getAllocationInterval} is used.
     *
     * @param actionDescription the ActionDescription inside which the ResourceAllocation is updated
     * @return the updated ActionDescription
     */
    public static ActionDescription.Builder updateResourceAllocationSlot(final ActionDescription.Builder actionDescription) {
        final ResourceAllocation.Builder resourceAllocationBuilder = actionDescription.getResourceAllocationBuilder();
        resourceAllocationBuilder.setSlot(getAllocationInterval(actionDescription));
        return actionDescription;
    }

    /**
     * Build an ActionReference from a given ActionDescription which can be added to an action chain.
     *
     * @param actionDescription the ActionDescription from which the ActionReference is generated
     * @return an ActionReference for the given ActionDescription
     */
    public static ActionReference getActionReferenceFromActionDescription(final ActionDescriptionOrBuilder actionDescription) {
        ActionReference.Builder actionReference = ActionReference.newBuilder();
        actionReference.setActionId(actionDescription.getId());
        actionReference.setAuthority(actionDescription.getActionAuthority());
        actionReference.setServiceStateDescription(actionDescription.getServiceStateDescription());
        return actionReference.build();
    }

    /**
     * Updates the ActionChain which is a description of actions that lead to this action.
     * The action chain is updated in a way that the immediate parent is the first element of
     * the chain. So the index of the chain indicates how many actions are in between this
     * action and the causing action.
     *
     * @param actionDescription the ActionDescription which is updated
     * @param parentAction      the ActionDescription of the action which is the cause for the new action
     * @return the updated ActionDescription
     */
    public static ActionDescription.Builder updateActionChain(final ActionDescription.Builder actionDescription, final ActionDescriptionOrBuilder parentAction) {
        actionDescription.addActionChain(getActionReferenceFromActionDescription(parentAction));
        actionDescription.addAllActionChain(parentAction.getActionChainList());
        return actionDescription;
    }

    /**
     * Check if the ResourceAllocation inside the ActionDescription has a token in its id field.
     *
     * @param actionDescription the ActionDescription which is checked
     * @return true if the id field contains a # which it the token separator and else false
     */
    public static boolean hasResourceAllocationToken(final ActionDescriptionOrBuilder actionDescription) {
        return actionDescription.getResourceAllocation().getId().contains(TOKEN_SEPARATOR);
    }

    /**
     * Add a token to the id field of the ResourceAllocation inside the ActionDescription.
     * This method does nothing if the id already contains a token.
     *
     * @param actionDescription the ActionDescription which is updated
     * @return the updated ActionDescription
     */
    public static ActionDescription.Builder generateToken(final ActionDescription.Builder actionDescription) {
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();
        if (hasResourceAllocationToken(actionDescription)) {
            return actionDescription;
        } else {
            String token = UUID.randomUUID().toString();
            resourceAllocation.setId(resourceAllocation.getId() + TOKEN_SEPARATOR + token);
            return actionDescription;
        }
    }

    /**
     * Get a new id value for the id field in the ResourceAllocation of an ActionDescription
     * while keeping the token if there si one.
     *
     * @param actionDescription the action description which is updated as described above
     * @return the action description which is updated as described above
     */
    public static ActionDescription.Builder updateResourceAllocationId(final ActionDescription.Builder actionDescription) {
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();
        String newId = UUID.randomUUID().toString();
        if (!hasResourceAllocationToken(actionDescription)) {
            resourceAllocation.setId(newId);
        } else {
            String token = resourceAllocation.getId().split(TOKEN_SEPARATOR)[1];
            resourceAllocation.setId(newId + TOKEN_SEPARATOR + token);
        }
        return actionDescription;
    }

    /**
     * Method generates a description for the given action pipeline.
     *
     * @param actionDescriptionCollection a collection of depending action descriptions.
     * @return a human readable description of the action pipeline.
     */
    public static String getDescription(final Collection<ActionDescription> actionDescriptionCollection) {
        String description = "";
        for (ActionDescription actionDescription : actionDescriptionCollection) {
            if (!description.isEmpty()) {
                description += " > ";
            }
            description += actionDescription.getDescription();
        }
        return description;
    }

    /**
     * Generates an action description according to the configuration of this unit remote.
     * The action description is generated using the ActionDescriptionProcessor.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param authorized       flag to define if this action should be authorized by the currently authenticated user or should be performed with OTHER rights.
     * @return the generated action description
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, final ServiceType serviceType, final boolean authorized) throws CouldNotPerformException {
        final ActionDescription.Builder actionDescriptionBuilder = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), detectInitiator());
        final Builder actionAuthorityBuilder = actionDescriptionBuilder.getActionAuthorityBuilder();

        if (authorized && SessionManager.getInstance().isLoggedIn()) {
            if (SessionManager.getInstance().getUserId() != null) {
                actionAuthorityBuilder.setUserId(SessionManager.getInstance().getUserId());
            } else {
                actionAuthorityBuilder.setUserId(SessionManager.getInstance().getClientId());
            }
        } else {
            actionAuthorityBuilder.setUserId("Other");
        }

        actionAuthorityBuilder.setAuthority(detectAuthority());
        return updateActionDescription(actionDescriptionBuilder, serviceAttribute, serviceType);
    }

    /**
     * Generates an action description according to the configuration of this unit remote.
     * The action description is generated using the ActionDescriptionProcessor.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @return the generated action description
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        return generateActionDescriptionBuilder(serviceAttribute, serviceType, true);
    }

    /**
     * Generates an action description according to the given attributes.
     * The action description is generated using the ActionDescriptionProcessor.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unitType         the service type according to the service attribute
     * @param authorized       flag to define if this action should be authorized by the currently authenticated user or should be performed with OTHER rights.
     * @return the generated action description
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, ServiceType serviceType, final UnitType unitType, final boolean authorized) throws CouldNotPerformException {

        // generate default description
        final ActionDescription.Builder actionDescriptionBuilder = generateActionDescriptionBuilder(serviceAttribute, serviceType, authorized);

        // update unit type
        actionDescriptionBuilder.getServiceStateDescriptionBuilder().setUnitType(unitType);

        // return
        return actionDescriptionBuilder;
    }

    /**
     * Generates an action description according to the given attributes.
     * The action description is generated using the ActionDescriptionProcessor.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unitType         the service type according to the service attribute
     * @return the generated action description
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, ServiceType serviceType, final UnitType unitType) throws CouldNotPerformException {
        return generateActionDescriptionBuilder(serviceAttribute, serviceType, unitType, true);
    }

    /**
     * Generates an action description according to the configuration of the given unit.
     * The action description is generated using the ActionDescriptionProcessor.
     * This method will set the service state description according to the service attribute and service type
     * and replace several keys in the description to make it human readable.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unit             the unit to control.
     * @param authorized       flag to define if this action should be authrorized by the currently authenticated user.
     * @return the generated action description
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilderAndUpdate(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit, final boolean authorized) throws CouldNotPerformException {
        return updateActionDescription(generateActionDescriptionBuilder(serviceAttribute, serviceType, authorized), serviceAttribute, serviceType, unit);
    }

    /**
     * Generates an action description according to the configuration of the given unit.
     * The action description is generated using the ActionDescriptionProcessor.
     * This method will set the service state description according to the service attribute and service type
     * and replace several keys in the description to make it human readable.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unit             the unit to control.
     * @return the generated action description
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilderAndUpdate(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit) throws CouldNotPerformException {
        return generateActionDescriptionBuilderAndUpdate(serviceAttribute, serviceType, unit, true);
    }

    /**
     * Update an action description according to the configuration of this unit remote.
     * The action description should be generated using the ActionDescriptionProcessor.
     * This method will set the service state description according to the service attribute and service type
     * and replace several keys in the description to make it human readable.
     *
     * @param actionDescription the action description which will be updated
     * @param serviceAttribute  the service attribute that will be applied by this action
     * @param serviceType       the service type according to the service attribute
     * @param unit              the unit to control.
     * @return the updated action description
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder updateActionDescription(final ActionDescription.Builder actionDescription, final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit) throws CouldNotPerformException {

        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();

        if (!actionDescription.hasDescription() || actionDescription.getDescription().isEmpty()) {
            actionDescription.setDescription(ActionDescriptionProcessor.GENERIC_ACTION_DESCRIPTION);
        }

        serviceStateDescription.setUnitId(unit.getId());
        resourceAllocation.addResourceIds(ScopeGenerator.generateStringRep(unit.getScope()));

        actionDescription.setDescription(actionDescription.getDescription().replace(ActionDescriptionProcessor.LABEL_KEY, unit.getLabel()));

        //TODO: provide label in different languages for LabelProcessor and replace according to user logged in
        if (actionDescription.hasLabel() && !LabelProcessor.isEmpty(actionDescription.getLabel())) {
            final String label = LabelProcessor.getBestMatch(actionDescription.getLabel());
            actionDescription.clearLabel();
            LabelProcessor.addLabel(actionDescription.getLabelBuilder(), Locale.ENGLISH, label.replace(ActionDescriptionProcessor.LABEL_KEY, unit.getLabel()));
        }

        return updateActionDescription(actionDescription, serviceAttribute, serviceType);
    }

    /**
     * Method detects if a human or the system is triggering this action.
     *
     * @return
     * @throws NotAvailableException
     */
    @Experimental
    public static Initiator detectInitiator() throws NotAvailableException {
        try {
            // because system instances are always logged in it has to be a human.
            if (!SessionManager.getInstance().isLoggedIn() || SessionManager.getInstance().getUserId() == null) {
                return Initiator.HUMAN;
            }

            if (Registries.getUnitRegistry().getUnitConfigById(SessionManager.getInstance().getUserId()).getUserConfig().getIsSystemUser()) {
                return Initiator.SYSTEM;
            } else {
                return Initiator.HUMAN;
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Initiator", ex);
        }
    }

    /**
     * Method detects if a user or the system is triggering this action.
     *
     * @return
     * @throws NotAvailableException
     */
    @Experimental
    public static Authority detectAuthority() throws NotAvailableException {
        try {
            // because system instances are always logged in it has to be a human.
            if (!SessionManager.getInstance().isLoggedIn() || SessionManager.getInstance().getUserId() == null) {
                return Authority.USER;
            }

            if (Registries.getUnitRegistry().getUnitConfigById(SessionManager.getInstance().getUserId()).getUserConfig().getIsSystemUser()) {
                return Authority.SYSTEM;
            } else {
                return Authority.USER;
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Authority", ex);
        }
    }

    /**
     * Update an action description according to the given service information.
     * This includes serializing the service attribute and replacing some keys if the action description has been
     * generated by the ActionDescriptionProcessor.
     *
     * @param actionDescription the action description that will be updated
     * @param serviceAttribute  the service attribute that will be applied by this action
     * @param serviceType       the service type according to the service attribute
     * @return the updated action description
     * @throws CouldNotPerformException if the service attribute cannot be verified or if the service attribute cannot
     *                                  be serialized
     */
    public static ActionDescription.Builder updateActionDescription(final ActionDescription.Builder actionDescription, final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        Services.verifyServiceState(serviceAttribute);

        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        ServiceJSonProcessor jSonProcessor = new ServiceJSonProcessor();

        serviceStateDescription.setServiceAttribute(jSonProcessor.serialize(serviceAttribute));
        serviceStateDescription.setServiceAttributeType(jSonProcessor.getServiceAttributeType(serviceAttribute));
        serviceStateDescription.setServiceType(serviceType);

        String description = actionDescription.getDescription();
        description = description.replace(ActionDescriptionProcessor.SERVICE_TYPE_KEY, StringProcessor.transformToCamelCase(serviceType.name()));

        // update authority if available
//        if (actionDescription.hasActionAuthority() && actionDescription.getActionAuthority().hasAuthority()) {
//            description = description.replace(ActionDescriptionProcessor.AUTHORITY_KEY, StringProcessor.transformToCamelCase(actionDescription.getActionAuthority().getAuthority().name()));
//        }

        // TODO: also replace SERVICE_ATTRIBUTE_KEY in description with a nice serviceAttribute representation
        String serviceAttributeRepresentation = StringProcessor.formatHumanReadable(serviceAttribute.toBuilder().clearField(ProtoBufFieldProcessor.getFieldDescriptor(serviceAttribute, Service.RESPONSIBLE_ACTION_FIELD_NAME)).build().toString());
        description = description.replace(ActionDescriptionProcessor.SERVICE_ATTRIBUTE_KEY, serviceAttributeRepresentation);
        if (actionDescription.hasLabel() && !LabelProcessor.isEmpty(actionDescription.getLabel())) {
            final String label = LabelProcessor.getBestMatch(actionDescription.getLabel());
            actionDescription.clearLabel();
            LabelProcessor.addLabel(actionDescription.getLabelBuilder(), Locale.ENGLISH, label.replace(ActionDescriptionProcessor.SERVICE_ATTRIBUTE_KEY, serviceAttributeRepresentation));
        }
        return actionDescription.setDescription(StringProcessor.removeDoubleWhiteSpaces(description));
    }

    public static ActionDescription getResponsibleAction(final MessageOrBuilder serviceState) throws NotAvailableException {
        try {
            return (ActionDescription) serviceState.getField(getResponsibleActionField(serviceState));
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("ActionDescription", ex);
        }
    }

    public static boolean hasResponsibleAction(final MessageOrBuilder serviceState) throws NotAvailableException {
        return serviceState.hasField(getResponsibleActionField(serviceState));
    }

    public static void clearResponsibleAction(final Message.Builder serviceState) throws NotAvailableException {
        serviceState.clearField(getResponsibleActionField(serviceState));
    }

    private static FieldDescriptor getResponsibleActionField(final MessageOrBuilder serviceState) throws NotAvailableException {
        FieldDescriptor fieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
        if (fieldDescriptor == null) {
            throw new NotAvailableException("responsible action for " + serviceState.getClass().getSimpleName());
        }
        return fieldDescriptor;
    }
}
