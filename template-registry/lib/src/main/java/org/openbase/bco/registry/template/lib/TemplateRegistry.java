package org.openbase.bco.registry.template.lib;

/*-
 * #%L
 * BCO Registry Template Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;
import org.openbase.type.domotic.registry.TemplateRegistryDataType.TemplateRegistryData;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;

import java.util.concurrent.Future;

public interface TemplateRegistry extends ActivityTemplateCollectionProvider, ServiceTemplateCollectionProvider, UnitTemplateCollectionProvider, DataProvider<TemplateRegistryData>, Shutdownable, RegistryService {

    // ===================================== UnitTemplate Methods =============================================================

    /**
     * Method updates the given unit template.
     *
     * @param unitTemplate the updated unit template.
     *
     * @return the updated unit template.
     */
    @RPCMethod
    Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate);


    /**
     * Method updates a unit template encoded in a transaction value.
     *
     * @param transactionValue the unit template to update in a transaction id.
     *
     * @return a transaction value containing the transaction id from the controller and the updated unit template encoded.
     */
    @RPCMethod
    Future<TransactionValue> updateUnitTemplateVerified(final TransactionValue transactionValue);

    /**
     * Method returns true if the underlying registry is marked as read only.
     *
     * @return if the unit template registry is read only
     */
    @RPCMethod
    Boolean isUnitTemplateRegistryReadOnly();

    /**
     * Method returns true if the underlying registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the unit template registry is consistent
     */
    @RPCMethod
    Boolean isUnitTemplateRegistryConsistent();


    // ===================================== ServiceTemplate Methods =============================================================

    /**
     * Method updates the given service template.
     *
     * @param serviceTemplate the updated service template.
     *
     * @return the updated service template.
     */
    @RPCMethod
    Future<ServiceTemplate> updateServiceTemplate(final ServiceTemplate serviceTemplate);

    /**
     * Method updates a service template encoded in a transaction value.
     *
     * @param transactionValue the service template to update in a transaction id.
     *
     * @return a transaction value containing the transaction id from the controller and the updated service template encoded.
     */
    @RPCMethod
    Future<TransactionValue> updateServiceTemplateVerified(final TransactionValue transactionValue);

    /**
     * Method returns true if the underlying registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the service template registry is consistent
     */
    @RPCMethod
    Boolean isServiceTemplateRegistryReadOnly();

    /**
     * Method returns true if the underlying registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the unit template registry is consistent
     */
    @RPCMethod
    Boolean isServiceTemplateRegistryConsistent();

    /**
     * Detects the service state class name attribute type for a service type. The service attribute type is the class name of
     * the service state.
     *
     * @param serviceType the service type for which the attribute type is retrieved.
     *
     * @return a string representing the service state used for the given service type.
     */
    default String getServiceStateClassName(final ServiceType serviceType) throws CouldNotPerformException {
        final ServiceTemplate serviceTemplate = getServiceTemplateByType(serviceType);
        final String communicationTypeName = StringProcessor.transformUpperCaseToPascalCase(serviceTemplate.getCommunicationType().name());
        return PowerState.class.getName().replaceAll(PowerState.class.getSimpleName(), communicationTypeName);
    }

    // ===================================== ActivityTemplate Methods =============================================================

    /**
     * Method updates the given activity template.
     *
     * @param activityTemplate the updated activity template.
     *
     * @return the updated activity template.
     */
    @RPCMethod
    Future<ActivityTemplate> updateActivityTemplate(ActivityTemplate activityTemplate);

    /**
     * Method updates a activity template encoded in a transaction value.
     *
     * @param transactionValue the activity template to update in a transaction id.
     *
     * @return a transaction value containing the transaction id from the controller and the updated activity template encoded.
     */
    @RPCMethod
    Future<TransactionValue> updateActivityTemplateVerified(final TransactionValue transactionValue);

    /**
     * Method returns true if the underlying registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the activity template registry is consistent
     */
    @RPCMethod
    Boolean isActivityTemplateRegistryReadOnly();

    /**
     * Method returns true if the underlying registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return if the activity template registry is consistent
     */
    @RPCMethod
    Boolean isActivityTemplateRegistryConsistent();


    /**
     * Method computes if the given {@code serviceType} supports the given {@code servicePattern}
     * which means it is registered as operation service within at least one unit template.
     *
     * @param serviceType the service type to check.
     * @param servicePattern the service to check for.
     *
     * @return true if the service type supports the service pattern, otherwise false.
     *
     * @throws CouldNotPerformException is thrown if the registry is not ready yet.
     */
    default boolean validateServicePatternSupport(final ServiceType serviceType, final ServicePattern servicePattern) throws CouldNotPerformException {
        for (UnitTemplate unitTemplate : getUnitTemplates()) {
            for (ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
                if (serviceDescription.getServiceType() == serviceType && serviceDescription.getPattern() == servicePattern) {
                    return true;
                }
            }
        }
        return false;
    }
}
