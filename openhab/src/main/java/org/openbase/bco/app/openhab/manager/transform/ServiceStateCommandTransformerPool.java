package org.openbase.bco.app.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.Message;
import org.eclipse.smarthome.core.types.Command;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceStateCommandTransformerPool {

    private static final String TRANSFORMER_CLASSNAME_POSTFIX = "Transformer";

    private static ServiceStateCommandTransformerPool instance;

    public static synchronized ServiceStateCommandTransformerPool getInstance() {
        if (instance == null) {
            instance = new ServiceStateCommandTransformerPool();
        }
        return instance;
    }

    private final Map<String, ServiceStateCommandTransformer> pool;

    private ServiceStateCommandTransformerPool() {
        this.pool = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <S extends Message, C extends Command> ServiceStateCommandTransformer<S, C> getTransformer(final Class<S> serviceStateClass, final Class<C> commandClass) throws NotAvailableException {
        return (ServiceStateCommandTransformer<S, C>) getTransformer(serviceStateClass.getSimpleName(), commandClass);
    }

    @SuppressWarnings("unchecked")
    public <S extends Message, C extends Command> ServiceStateCommandTransformer<S, C> getTransformer(final ServiceType serviceType, final C command) throws CouldNotPerformException {
        return (ServiceStateCommandTransformer<S, C>) getTransformer(serviceType, command.getClass());
    }

    @SuppressWarnings("unchecked")
    public <S extends Message, C extends Command> ServiceStateCommandTransformer<S, C> getTransformer(final ServiceType serviceType, final Class<C> commandClass) throws CouldNotPerformException {
        final String serviceStateName = StringProcessor.transformToCamelCase(
                Registries.getTemplateRegistry().getServiceTemplateByType(serviceType).getCommunicationType().name());
        return (ServiceStateCommandTransformer<S, C>) getTransformer(serviceStateName, commandClass);
    }

    @SuppressWarnings("unchecked")
    private <S extends Message, C extends Command> ServiceStateCommandTransformer<S, C> getTransformer(final String serviceStateName, final Class<C> commandClass) throws NotAvailableException {
        final String simpleClassName = serviceStateName + commandClass.getSimpleName() + TRANSFORMER_CLASSNAME_POSTFIX;
        if (!pool.containsKey(simpleClassName)) {
            pool.put(simpleClassName, loadTransformer(simpleClassName));
        }

        return (ServiceStateCommandTransformer<S, C>) pool.get(simpleClassName);
    }

    private ServiceStateCommandTransformer loadTransformer(final String simpleClassName) throws NotAvailableException {
        final String className = getClass().getPackage().getName() + "." + simpleClassName;
        try {
            return (ServiceStateCommandTransformer) getClass().getClassLoader().loadClass(className).getConstructor().newInstance();
        } catch (InstantiationException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException ex) {
            throw new NotAvailableException(simpleClassName, ex);
        }
    }
}
