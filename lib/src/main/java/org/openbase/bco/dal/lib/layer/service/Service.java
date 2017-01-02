package org.openbase.bco.dal.lib.layer.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.action.ActionConfigType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;

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
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Service {

    public static final String SERVICE_LABEL = Service.class.getSimpleName();

    public Future<Void> applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException;

    /**
     * This method returns the service base name of the given service type.
     *
     * The base name is the service name without service suffix.
     * e.g. the base name of service PowerStateService is PowerState.
     *
     * @param serviceType the service type to extract the base name.
     * @return the service base name.
     */
    public static String getServiceBaseName(ServiceTemplate.ServiceType serviceType) {
        return StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll(Service.SERVICE_LABEL, "");
    }

    public static String getServicePrefix(final ServiceTemplateType.ServiceTemplate.ServicePattern pattern) throws CouldNotPerformException {
        switch (pattern) {
            case CONSUMER:
                return "";
            case OPERATION:
                return "set";
            case PROVIDER:
                return "get";
            default:
                throw new NotSupportedException(pattern, Service.class);
        }
    }

    /**
     * Method returns the state-name of the appurtenant service.
     *
     * @param template The serviceTemplate.
     *
     * @return The state-name as string.
     */
    public static String getStateOfService(final ServiceTemplateType.ServiceTemplate template) {
        return template.getType().name().toLowerCase().replaceAll("_service", "");
    }

    public static Method detectServiceMethod(final ServiceTemplateType.ServiceTemplate template, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        try {
            return instanceClass.getMethod(getServicePrefix(template.getPattern()) + StringProcessor.transformUpperCaseToCamelCase(template.getType().name()).replaceAll("Service", ""), argumentClasses);
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new CouldNotPerformException("Could not detect service method!", ex);
        }
    }

    public static Object invokeServiceMethod(final ServiceTemplateType.ServiceTemplate template, final Service instance, final Object... arguments) throws CouldNotPerformException {
        try {
            return detectServiceMethod(template, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
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
