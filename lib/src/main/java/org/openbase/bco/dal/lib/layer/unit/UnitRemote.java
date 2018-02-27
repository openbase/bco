package org.openbase.bco.dal.lib.layer.unit;

/*
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

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.pattern.ConfigurableRemote;
import rsb.Scope;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 * @param <M> Message
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitRemote<M extends GeneratedMessage> extends Unit<M>, ConfigurableRemote<String, M, UnitConfig> {

    /**
     * Method initializes this unit remote instance via it's remote controller scope.
     *
     * @param scope the scope which is used to reach the remote controller.
     * @throws InitializationException is thrown in case the remote could not be initialized with the given scope.
     * @throws InterruptedException    is thrown in case the thread is externally interrupted.
     */
    void init(ScopeType.Scope scope) throws InitializationException, InterruptedException;

    /**
     * Method initializes this unit remote instance via it's remote controller scope.
     *
     * @param scope the scope which is used to reach the remote controller.
     * @throws InitializationException is thrown in case the remote could not be initialized with the given scope.
     * @throws InterruptedException    is thrown in case the thread is externally interrupted.
     */
    void init(Scope scope) throws InitializationException, InterruptedException;

    /**
     * Method initializes this unit remote instance via the given id.
     *
     * @param id the unit id which is used to resolve the remote controller scope.
     * @throws InitializationException is thrown in case the remote could not be initialized with the given id.
     * @throws InterruptedException    is thrown in case the thread is externally interrupted.
     */
    void initById(final String id) throws InitializationException, InterruptedException;

    /**
     * Method initializes this unit remote instance via the given label.
     *
     * @param label the unit label which is used to resolve the remote controller scope.
     * @throws InitializationException is thrown in case the remote could not be initialized with the given label.
     * @throws InterruptedException    is thrown in case the thread is externally interrupted.
     */
    void initByLabel(final String label) throws InitializationException, InterruptedException;

    /**
     * This method returns if the unit of this remote is enabled.
     * An unit is marked as disabled if the related unit host is not available. For instance all units are automatically disabled when the providing device is currently borrowed or at least marked as not installed.
     * <p>
     * Note: Method returns false if the state could not be detected. This can happen if the unit was never initialized or the related unit configuration is not available.
     *
     * @return returns true if the unit is enabled otherwise false.
     */
    boolean isEnabled();

    /**
     * Set the session manager for a unit remote. The session manager is
     * used to determine who triggers actions using the unit remote.
     *
     * @param sessionManager the session manager containing authorization information for the usage of the remote.
     */
    void setSessionManager(SessionManager sessionManager);

    /**
     * Get the current session manager of the unit remote.
     *
     * @return the current session manager.
     */
    SessionManager getSessionManager();

    /**
     * Update an action description according to the configuration of this unit remote.
     * The action description should be generated using the ActionDescriptionProcessor.
     * This method will set the service state description according to the service attribute and service type
     * and replace several keys in the description to make is human readable.
     *
     * @param actionDescription the action description which will be updated
     * @param serviceAttribute  the service attribute that will be applied by this action
     * @param serviceType       the service type according to the service attribute
     * @return the updated action description
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    default ActionDescription.Builder updateActionDescription(final ActionDescription.Builder actionDescription, final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();

        serviceStateDescription.setUnitId(getId());
        resourceAllocation.addResourceIds(ScopeGenerator.generateStringRep(getScope()));

        actionDescription.setDescription(actionDescription.getDescription().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));
        try {
            String username = "";
            if (getSessionManager().getUserId() != null) {
                username += Registries.getUnitRegistry().getUnitConfigById(getSessionManager().getUserId()).getUserConfig().getUserName();
            }
            if (getSessionManager().getClientId() != null) {
                if (!username.isEmpty()) {
                    username += "@";
                }
                username += Registries.getUnitRegistry().getUnitConfigById(getSessionManager().getClientId()).getUserConfig().getUserName();
            }
            if (username.isEmpty()) {
                username = "Other";
            }
            actionDescription.setDescription(actionDescription.getDescription().replace(ActionDescriptionProcessor.AUTHORITY_KEY, username));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        actionDescription.setLabel(actionDescription.getLabel().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));

        return Services.updateActionDescription(actionDescription, serviceAttribute, serviceType);
    }

//    /**
//     * Update an action description according to the configuration of this unit remote.
//     * The action description should be generated using the ActionDescriptionProcessor.
//     * This method will set the service state description according to the service attribute and service type
//     * and replace several keys in the description to make is human readable.
//     * This method tries to automatically resolve the service type for a given service attribute.
//     *
//     * @param actionDescription the action description which will be updated
//     * @param serviceAttribute  the service attribute that will be applied by this action
//     * @return the updated action description
//     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
//     *                                  verified or serialized or if the service type cannot be resolved
//     */
//    default ActionDescription.Builder updateActionDescription(final ActionDescription.Builder actionDescription, final Message serviceAttribute) throws CouldNotPerformException {
//        return updateActionDescription(actionDescription, serviceAttribute, Services.getServiceType(serviceAttribute));
//    }
}
