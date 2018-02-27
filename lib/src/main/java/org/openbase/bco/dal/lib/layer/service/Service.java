package org.openbase.bco.dal.lib.layer.service;

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
import java.lang.reflect.Method;
import java.util.Collection;

import com.google.protobuf.Message;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.mode.OperationModeType;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ContactStateType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @author <a href="mailto:agatting@techfak.uni-bielefeld.de">Andreas Gatting</a>
 */
public interface Service {

    Package SERVICE_STATE_PACKAGE = ContactStateType.class.getPackage();
    Package SERVICE_MODE_PACKAGE = OperationModeType.OperationMode.class.getPackage();
    String SERVICE_LABEL = Service.class.getSimpleName();
    
    String RESPONSIBLE_ACTION_FIELD_NAME = "responsible_action";
    
    /**
     * This method returns the service base name of the given service type.
     *
     * The base name is the service name without service suffix.
     * e.g. the base name of service PowerStateService is PowerState.
     *
     * @param serviceType the service type to extract the base name.
     * @return the service base name.
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static String getServiceBaseName(ServiceTemplate.ServiceType serviceType) {
        return Services.getServiceBaseName(serviceType);
    }

    /**
     *
     * @param pattern
     * @return
     * @throws CouldNotPerformException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static String getServicePrefix(final ServiceTemplateType.ServiceTemplate.ServicePattern pattern) throws CouldNotPerformException {
        return Services.getServicePrefix(pattern);
    }

    /**
     * Method returns the state name of the appurtenant service.
     *
     * @param serviceType the service type which is used to generate the service name.
     * @return The state type name as string.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown in case the given serviceType is null.
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static String getServiceStateName(final ServiceType serviceType) throws NotAvailableException {
        return Services.getServiceStateName(serviceType);
    }

    /**
     * Method returns the state name of the appurtenant service.
     *
     * @param template The service template.
     * @return The state type name as string.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown in case the given template is null.
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static String getServiceStateName(final ServiceTemplate template) throws NotAvailableException {
        return Services.getServiceStateName(template);
    }

    /**
     * Method returns a collection of service state values.
     *
     * @param serviceType the service type to identify the service state class.
     * @return a collection of enum values of the service state.
     * @throws NotAvailableException is thrown in case the referred service state does not contain any state values.
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Collection<? extends Enum> getServiceStateValues(final ServiceType serviceType) throws NotAvailableException {
        return Services.getServiceStateValues(serviceType);
    }

    /**
     * Method builds a new service state related to the given service type and initializes this instance with the given state value.
     *
     * @param <SC> the service class of the service state.
     * @param <SV> the state enum of the service.
     * @param serviceType the service type of the service state.
     * @param stateValue a compatible state value related to the given service state.
     * @return a new service state initialized with the state value.
     * @throws CouldNotPerformException is thrown in case the given arguments are not compatible with each other or something else went wrong during the build.
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static <SC extends GeneratedMessage, SV extends Enum> SC buildServiceState(final ServiceType serviceType, SV stateValue) throws CouldNotPerformException {
        return Services.buildServiceState(serviceType, stateValue);
    }

    /**
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services.getServiceStateClass(...)} instead.
     */
    @Deprecated
    static Class<? extends GeneratedMessage> detectServiceDataClass(final ServiceType serviceType) throws NotAvailableException {
        return Services.getServiceStateClass(serviceType);
    }

    /**
     * Method detects and returns the service state class.
     *
     * @param serviceType the given service type to resolve the class.
     * @return the service state class.
     * @throws NotAvailableException is thrown in case the class could not be detected.
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Class<? extends GeneratedMessage> getServiceStateClass(final ServiceType serviceType) throws NotAvailableException {
        return Services.getServiceStateClass(serviceType);
    }

    /**
     *
     * @param serviceType
     * @param servicePattern
     * @param instanceClass
     * @param argumentClasses
     * @return
     * @throws CouldNotPerformException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Method detectServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return Services.detectServiceMethod(serviceType, servicePattern, instanceClass, argumentClasses);
    }

    /**
     *
     * @param description
     * @param instanceClass
     * @param argumentClasses
     * @return
     * @throws CouldNotPerformException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Method detectServiceMethod(final ServiceDescription description, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return Services.detectServiceMethod(description, instanceClass, argumentClasses);
    }


    /**
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Object invokeServiceMethod(final ServiceDescription description, final Service instance, final Object... arguments) throws CouldNotPerformException {
        return invokeServiceMethod(description, instance, arguments);
    }

    /**
     *
     * @param serviceType
     * @param servicePattern
     * @param instance
     * @param arguments
     * @return
     * @throws CouldNotPerformException
     * @throws NotSupportedException
     * @throws IllegalArgumentException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Object invokeServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final Object instance, final Object... arguments) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        return Services.invokeServiceMethod(serviceType, servicePattern, instance, arguments);
    }

    /**
     *
     * @param serviceType
     * @param instance
     * @param arguments
     * @return
     * @throws CouldNotPerformException
     * @throws NotSupportedException
     * @throws IllegalArgumentException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Object invokeProviderServiceMethod(final ServiceType serviceType, final Object instance, final Object... arguments) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        return Services.invokeProviderServiceMethod(serviceType, instance, arguments);
    }

    /**
     *
     * @param serviceType
     * @param instance
     * @param arguments
     * @return
     * @throws CouldNotPerformException
     * @throws NotSupportedException
     * @throws IllegalArgumentException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Object invokeOperationServiceMethod(final ServiceType serviceType, final Object instance, final Object... arguments) throws CouldNotPerformException, NotSupportedException, IllegalArgumentException {
        return Services.invokeOperationServiceMethod(serviceType, instance, arguments);
    }

    /**
     *
     * @param arguments
     * @return
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static Class[] getArgumentClasses(final Object[] arguments) {
        return Services.getArgumentClasses(arguments);
    }

    /**
     *
     * @param actionDescription
     * @param serviceAttribue
     * @param serviceType
     * @return
     * @throws CouldNotPerformException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static ActionDescription.Builder upateActionDescription(final ActionDescription.Builder actionDescription, final Object serviceAttribue, final ServiceType serviceType) throws CouldNotPerformException {
        return Services.updateActionDescription(actionDescription, (Message) serviceAttribue, serviceType);
    }

    /**
     *
     * @param actionDescription
     * @param serviceAttribue
     * @return
     * @throws CouldNotPerformException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static ActionDescription.Builder upateActionDescription(final ActionDescription.Builder actionDescription, final Object serviceAttribue) throws CouldNotPerformException {
        return Services.updateActionDescription(actionDescription, (Message) serviceAttribue);
    }

    /**
     *
     * @param serviceAttribute
     * @return
     * @throws CouldNotPerformException
     * @deprecated please use the methods provided by {@code org.openbase.bco.dal.lib.layer.service.Services} instead.
     */
    @Deprecated
    static ServiceType getServiceType(final Object serviceAttribute) throws CouldNotPerformException {
        return Services.getServiceType(serviceAttribute);
    }
}
