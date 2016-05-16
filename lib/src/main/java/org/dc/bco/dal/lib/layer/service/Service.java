package org.dc.bco.dal.lib.layer.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotSupportedException;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

    //TODO add to rst
    public enum ServiceBaseType {

        PROVIDER, CONSUMER, OPERATION
    };

    public Future<Void> applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException;

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
