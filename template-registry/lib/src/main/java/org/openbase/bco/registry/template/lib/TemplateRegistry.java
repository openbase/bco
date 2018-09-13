package org.openbase.bco.registry.template.lib;

/*-
 * #%L
 * BCO Registry Template Library
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

import org.openbase.bco.registry.lib.provider.template.ActivityTemplateCollectionProvider;
import org.openbase.bco.registry.lib.provider.template.ServiceTemplateCollectionProvider;
import org.openbase.bco.registry.lib.provider.template.UnitTemplateCollectionProvider;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.RegistryService;
import rst.domotic.activity.ActivityTemplateType.ActivityTemplate;
import rst.domotic.communication.TransactionValueType.TransactionValue;
import rst.domotic.registry.TemplateRegistryDataType.TemplateRegistryData;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;

import java.util.concurrent.Future;

public interface TemplateRegistry extends ActivityTemplateCollectionProvider, ServiceTemplateCollectionProvider, UnitTemplateCollectionProvider, DataProvider<TemplateRegistryData>, Shutdownable, RegistryService {

    // ===================================== UnitTemplate Methods =============================================================

    /**
     * Method updates the given unit template.
     *
     * @param unitTemplate the updated unit template.
     *
     * @return the updated unit template.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;


    /**
     * Method updates a unit template encoded in a transaction value.
     *
     * @param transactionValue the unit template to update in a transaction id.
     *
     * @return a transaction value containing the transaction id from the controller and the updated unit template encoded.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> updateUnitTemplateVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as read only.
     *
     * @return if the unit template registry is read only
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the unit template registry is consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException;


    // ===================================== ServiceTemplate Methods =============================================================

    /**
     * Method updates the given service template.
     *
     * @param serviceTemplate the updated service template.
     *
     * @return the updated service template.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<ServiceTemplate> updateServiceTemplate(final ServiceTemplate serviceTemplate) throws CouldNotPerformException;

    /**
     * Method updates a service template encoded in a transaction value.
     *
     * @param transactionValue the service template to update in a transaction id.
     *
     * @return a transaction value containing the transaction id from the controller and the updated service template encoded.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> updateServiceTemplateVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the service template registry is consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isServiceTemplateRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the unit template registry is consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isServiceTemplateRegistryConsistent() throws CouldNotPerformException;

    /**
     * Construct the service attribute type for a service type. The service attribute type is the class name of
     * the service state.
     *
     * @param serviceType the service type for which the attribute type is retrieved.
     *
     * @return a string representing the service state used for the given service type.
     *
     * @throws CouldNotPerformException if the service template for the service type is not available.
     */
    default String getServiceAttributeType(final ServiceType serviceType) throws CouldNotPerformException {
        final ServiceTemplate serviceTemplate = getServiceTemplateByType(serviceType);
        final String communicationTypeName = StringProcessor.transformUpperCaseToCamelCase(serviceTemplate.getCommunicationType().name());
        return PowerState.class.getName().replaceAll(PowerState.class.getSimpleName(), communicationTypeName);
    }

    // ===================================== ActivityTemplate Methods =============================================================

    /**
     * Method updates the given activity template.
     *
     * @param activityTemplate the updated activity template.
     *
     * @return the updated activity template.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<ActivityTemplate> updateActivityTemplate(ActivityTemplate activityTemplate) throws CouldNotPerformException;

    /**
     * Method updates a activity template encoded in a transaction value.
     *
     * @param transactionValue the activity template to update in a transaction id.
     *
     * @return a transaction value containing the transaction id from the controller and the updated activity template encoded.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<TransactionValue> updateActivityTemplateVerified(final TransactionValue transactionValue) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the activity template registry is consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isActivityTemplateRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as consistent.
     *
     * @return if the activity template registry is consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isActivityTemplateRegistryConsistent() throws CouldNotPerformException;
}
