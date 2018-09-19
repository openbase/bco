package org.openbase.bco.dal.lib.action;

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.annotation.Experimental;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionDescriptionType.ActionDescriptionOrBuilder;
import rst.domotic.action.ActionInitiatorType.ActionInitiator;
import rst.domotic.action.ActionParameterType.ActionParameter;
import rst.domotic.action.ActionParameterType.ActionParameterOrBuilder;
import rst.domotic.action.ActionPriorityType.ActionPriority.Priority;
import rst.domotic.action.ActionReferenceType.ActionReference;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Collection;

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

    private static final ServiceJSonProcessor JSON_PROCESSOR = new ServiceJSonProcessor();

    public static ActionParameter.Builder generateDefaultActionParameter(final Message serviceAttribute, final ServiceType serviceType, final UnitType unitType) throws CouldNotPerformException {
        return generateDefaultActionParameter(generateServiceStateDescription(serviceAttribute, serviceType).setUnitType(unitType).build(), true);
    }

    public static ActionParameter.Builder generateDefaultActionParameter(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit) throws CouldNotPerformException {
        return generateDefaultActionParameter(generateServiceStateDescription(serviceAttribute, serviceType, unit), true);
    }

    public static ActionParameter.Builder generateDefaultActionParameter(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit, final boolean authenticated) throws CouldNotPerformException {
        return generateDefaultActionParameter(generateServiceStateDescription(serviceAttribute, serviceType, unit), authenticated);
    }

    public static ActionParameter.Builder generateDefaultActionParameter(final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        return generateDefaultActionParameter(generateServiceStateDescription(serviceAttribute, serviceType).build(), true);
    }


    public static ActionParameter.Builder generateDefaultActionParameter(final ServiceStateDescription serviceStateDescription) {
        return generateDefaultActionParameter(serviceStateDescription, true);
    }

    /**
     * Generates a message of default {@code ActionParameter}.
     * <p>
     * These are:
     * <ul>
     * <li>Priority = NORMAL</li>
     * <li>ExecutionTimePeriod = 0</li>
     * </ul>
     *
     * @param serviceStateDescription the description of which service how to manipulate.
     *
     * @return an ActionParameter type with the described values.
     */
    public static ActionParameter.Builder generateDefaultActionParameter(final ServiceStateDescription serviceStateDescription, final boolean authenticated) {
        ActionParameter.Builder actionParameter = ActionParameter.newBuilder();
        actionParameter.setServiceStateDescription(serviceStateDescription);
        actionParameter.setPriority(Priority.NORMAL);
        actionParameter.setExecutionTimePeriod(0);
        actionParameter.setInitiator(detectActionInitiator(authenticated));
        return actionParameter;
    }

    /**
     * Build an ActionReference from a given ActionDescription which can be added to an action chain.
     *
     * @param actionDescription the ActionDescription from which the ActionReference is generated.
     *
     * @return an ActionReference for the given ActionDescription.
     */
    public static ActionReference generateActionReference(final ActionDescriptionOrBuilder actionDescription) {
        ActionReference.Builder actionReference = ActionReference.newBuilder();
        actionReference.setActionId(actionDescription.getId());
        actionReference.setInitiator(actionDescription.getInitiator());
        actionReference.setServiceStateDescription(actionDescription.getServiceStateDescription());
        return actionReference.build();
    }

    /**
     * Build an ActionReference from a given ActionParameter which can be added to an action chain.
     *
     * @param actionParameter the ActionParameter from which the ActionReference is generated.
     *
     * @return an ActionReference for the given ActionParameter.
     */
    public static ActionReference generateActionReference(final ActionParameterOrBuilder actionParameter) {
        ActionReference.Builder actionReference = ActionReference.newBuilder();
        actionReference.setActionId(actionParameter.getInitiator().getUnitId());
        actionReference.setInitiator(actionParameter.getInitiator());
        actionReference.setServiceStateDescription(actionParameter.getServiceStateDescription());
        return actionReference.build();
    }

    /**
     * Updates the ActionChain which is a description of actions that lead to this action.
     * The action chain is updated in a way that the immediate parent is the first element of
     * the chain. The index of the chain indicates how many actions are in between this
     * action and the causing action.
     *
     * @param actionDescription the ActionDescription which is updated
     * @param parentAction      the ActionDescription of the action which is the cause for the new action
     *
     * @return the updated ActionDescription
     */
    public static ActionDescription.Builder updateActionChain(final ActionDescription.Builder actionDescription, final ActionDescriptionOrBuilder parentAction) {
        actionDescription.clearActionChain();
        actionDescription.addActionChain(generateActionReference(parentAction));
        actionDescription.addAllActionChain(parentAction.getActionChainList());
        return actionDescription;
    }

    /**
     * Method generates a description for the given action chain.
     *
     * @param actionDescriptionCollection a collection of depending action descriptions.
     *
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


//    /**
//     * Generates an action description according to the configuration of this unit remote.
//     * The action description is generated using the ActionDescriptionProcessor.
//     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
//     *
//     * @param serviceAttribute the service attribute that will be applied by this action
//     * @param serviceType      the service type according to the service attribute
//     * @param authorized       flag to define if this action should be authorized by the currently authenticated user or should be performed with OTHER rights.
//     *
//     * @return the generated action description
//     *
//     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
//     *                                  verified or serialized
//     */
//    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, final ServiceType serviceType, final boolean authorized) throws CouldNotPerformException {
//        final ActionDescription.Builder actionDescriptionBuilder = ActionDescriptionProcessor.generateActionDescriptionBuilder(serviceAttribute, serviceType);
//        actionDescriptionBuilder.setInitiator(detectActionInitiator(authorized));
//        return updateActionDescription(actionDescriptionBuilder, serviceAttribute, serviceType);
//    }
//
    /**
     * Generates an action description according to the configuration of this unit remote.
     * The action description is generated using the ActionDescriptionProcessor.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     *
     * @return the generated action description
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        return generateActionDescriptionBuilder(generateDefaultActionParameter(serviceAttribute, serviceType));
    }
//
//    /**
//     * Generates an action description according to the given attributes.
//     * The action description is generated using the ActionDescriptionProcessor.
//     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
//     *
//     * @param serviceAttribute the service attribute that will be applied by this action
//     * @param serviceType      the service type according to the service attribute
//     * @param unitType         the service type according to the service attribute
//     * @param authorized       flag to define if this action should be authorized by the currently authenticated user or should be performed with OTHER rights.
//     *
//     * @return the generated action description
//     *
//     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
//     *                                  verified or serialized
//     */
//    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, ServiceType serviceType, final UnitType unitType, final boolean authorized) throws CouldNotPerformException {
//
//        // generate default description
//        final ActionDescription.Builder actionDescriptionBuilder = generateActionDescriptionBuilder(serviceAttribute, serviceType, authorized);
//
//        // update unit type
//        actionDescriptionBuilder.getServiceStateDescriptionBuilder().setUnitType(unitType);
//
//        // return
//        return actionDescriptionBuilder;
//    }
//
    /**
     * Generates an action description according to the given attributes.
     * The action description is generated using the ActionDescriptionProcessor.
     * Additionally the initiator is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unitType         the service type according to the service attribute
     *
     * @return the generated action description
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, ServiceType serviceType, final UnitType unitType) throws CouldNotPerformException {
        return generateActionDescriptionBuilder(generateDefaultActionParameter(serviceAttribute, serviceType, unitType));
    }
//
//    /**
//     * Generates an action description according to the configuration of the given unit.
//     * The action description is generated using the ActionDescriptionProcessor.
//     * This method will set the service state description according to the service attribute and service type
//     * and replace several keys in the description to make it human readable.
//     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
//     *
//     * @param serviceAttribute the service attribute that will be applied by this action
//     * @param serviceType      the service type according to the service attribute
//     * @param unit             the unit to control.
//     * @param authorized       flag to define if this action should be authrorized by the currently authenticated user.
//     *
//     * @return the generated action description
//     *
//     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
//     *                                  verified or serialized
//     */
//    public static ActionDescription.Builder generateActionDescriptionBuilderAndUpdate(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit, final boolean authorized) throws CouldNotPerformException {
//        return updateActionDescription(generateActionDescriptionBuilder(serviceAttribute, serviceType, authorized), serviceAttribute, serviceType, unit);
//    }
//
    /**
     * Generates an action description according to the configuration of the given unit.
     * The action description is generated using the ActionDescriptionProcessor.
     * This method will set the service state description according to the service attribute and service type.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unit             the unit to control.
     *
     * @return the generated action description
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit) throws CouldNotPerformException {
        return generateActionDescriptionBuilder(generateDefaultActionParameter(serviceAttribute, serviceType, unit));
    }

    /**
     * Generates an {@code ActionDescription} which is based on the given {@code ActionParameter}.
     *
     * @param actionParameter type which contains all needed parameters to generate an {@code ActionDescription}
     *
     * @return an {@code ActionDescription} that only misses unit and service information
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final ActionParameterOrBuilder actionParameter) {
        ActionDescription.Builder actionDescription = ActionDescription.newBuilder();

        // add values from ActionParameter
        actionDescription.addAllCategory(actionParameter.getCategoryList());
        actionDescription.setLabel(actionParameter.getLabel());
        actionDescription.setInitiator(actionParameter.getInitiator());
        actionDescription.setServiceStateDescription(actionParameter.getServiceStateDescription());
        actionDescription.setExecutionTimePeriod(actionParameter.getExecutionTimePeriod());
        actionDescription.setPriority(actionParameter.getPriority());

        // if an initiator action is defined in ActionParameter the actionChain is updated
        if (actionParameter.hasCause()) {
            updateActionChain(actionDescription, actionParameter.getCause());
        }

        return actionDescription;
    }

    /**
     * Update an action description according to the configuration of this unit remote.
     * This method will set the service state description according to the service attribute and service type.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unit             the unit to control.
     *
     * @return the action description
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ServiceStateDescription generateServiceStateDescription(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit) throws CouldNotPerformException {
        return generateServiceStateDescription(serviceAttribute, serviceType).setUnitId(unit.getId()).build();
    }

    /**
     * Update an action description according to the configuration of this unit remote.
     * This method will set the service state description according to the service attribute and service type.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     *
     * @return the action description builder
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ServiceStateDescription.Builder generateServiceStateDescription(final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        ServiceStateDescription.Builder serviceStateDescriptionBuilder = ServiceStateDescription.newBuilder();
        serviceStateDescriptionBuilder.setServiceAttribute(JSON_PROCESSOR.serialize(Services.verifyAndRevalidateServiceState(serviceAttribute)));
        serviceStateDescriptionBuilder.setServiceAttributeType(JSON_PROCESSOR.getServiceAttributeType(serviceAttribute));
        serviceStateDescriptionBuilder.setServiceType(serviceType);
        return serviceStateDescriptionBuilder;
    }

    /**
     * Method detects if a human or the system is triggering this action.
     *
     * @return
     *
     * @throws NotAvailableException
     */
    @Experimental
    public static ActionInitiator detectActionInitiator(final boolean authorized) {
        final ActionInitiator.Builder actionInitiatorBuilder = ActionInitiator.newBuilder();
        if (authorized && SessionManager.getInstance().isLoggedIn()) {
            if (SessionManager.getInstance().getUserId() != null) {
                actionInitiatorBuilder.setUnitId(SessionManager.getInstance().getUserId());
            } else {
                actionInitiatorBuilder.setUnitId(SessionManager.getInstance().getClientId());
            }
        } else {
            actionInitiatorBuilder.clearUnitId();
        }
        return actionInitiatorBuilder.build();
    }
}
