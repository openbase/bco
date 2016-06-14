package org.openbase.bco.dal.lib.layer.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotSupportedException;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.service.ServiceTemplateType;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
 *
 * @author Divine Threepwood
 */
public interface Service {

    public static final String SERVICE_LABEL = Service.class.getSimpleName();
    public static final String PROVIDER_SERVICE_LABEL = ProviderService.class.getSimpleName();
    public static final String CONSUMER_SERVICE_LABEL = ConsumerService.class.getSimpleName();
    public static final String OPERATION_SERVICE_LABEL = OperationService.class.getSimpleName();

    //TODO add to rst
    public enum ServiceBaseType {

        PROVIDER, CONSUMER, OPERATION
    };

    public Future<Void> applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException;

    /**
     * This method returns the service base name of the given service type.
     *
     * The base name is the service name without service suffix.
     * e.g. the base name of service PowerStateProviderService is PowerState.
     *
     * @param serviceType the service type to extract the base name.
     * @return the service base name.
     */
    public static String getServiceBaseName(ServiceTemplate.ServiceType serviceType) {
        return StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll(Service.PROVIDER_SERVICE_LABEL, "").replaceAll(Service.CONSUMER_SERVICE_LABEL, "").replaceAll(Service.OPERATION_SERVICE_LABEL, "");
    }

    public static ServiceBaseType detectServiceBaseType(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException {
        if (serviceType.name().endsWith(ServiceBaseType.PROVIDER.name())) {
            return ServiceBaseType.PROVIDER;
        } else if (serviceType.name().endsWith("SERVICE")) {
            return ServiceBaseType.OPERATION;
        } else if (serviceType.name().endsWith(ServiceBaseType.PROVIDER.name())) {
            return ServiceBaseType.CONSUMER;
        } else {
            throw new CouldNotPerformException("Could not detect service base type!");
        }
    }

    public static ServiceTemplate.ServiceType getProviderForOperationService(final ServiceTemplateType.ServiceTemplate.ServiceType operationServiceType) throws CouldNotPerformException {
        try {
            assert detectServiceBaseType(operationServiceType).equals(ServiceBaseType.OPERATION);
            return ServiceTemplate.ServiceType.valueOf(operationServiceType.name().replaceFirst(ServiceBaseType.PROVIDER.name(), "SERVICE"));
        } catch (IllegalArgumentException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not detect ProviderService of related OperationService[" + operationServiceType + "]", ex);
        }
    }

    public static String getServicePrefix(final ServiceTemplateType.ServiceTemplate.ServiceType type) throws CouldNotPerformException {
        switch (detectServiceBaseType(type)) {
        case CONSUMER:
            return "";
        case OPERATION:
            return "set";
        case PROVIDER:
            return "get";
        default:
            throw new NotSupportedException(type, Service.class);
        }
    }

    public static Method detectServiceMethod(final ServiceTemplateType.ServiceTemplate.ServiceType type, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        try {
            return instanceClass.getMethod(getServicePrefix(type) + StringProcessor.transformUpperCaseToCamelCase(type.name()).replaceAll("Service", ""), argumentClasses);
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new CouldNotPerformException("Could not detect service method!", ex);
        }
    }

    public static Object invokeServiceMethod(final ServiceTemplateType.ServiceTemplate.ServiceType type, final Service instance, final Object... arguments) throws CouldNotPerformException {
        try {
            return detectServiceMethod(type, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new CouldNotPerformException("Could not invoke service method!", ex);
        }
    }

    public static Class[] getArgumentClasses(final Object[] arguments) {
        Class[] classes = new Class[arguments.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = arguments[i].getClass();
        }
        return classes;
    }
}
