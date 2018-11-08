package org.openbase.bco.device.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.Command;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class building a mapping from service types to command classes from the service template registry.
 * The mapping will be build on the first call and then register an observer on the registry to update automatically
 * on changes.
 * <p>
 * Note: if you register an observer on the template registry and in this need to resolve the command classes
 * you should make to sure to call {@link #getCommandClasses(ServiceType)} before because only after this call
 * this helper registers its observer which will then be executed before yours.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTypeCommandMapping {

    public static final String COMMAND_CLASSES_KEY = "COMMAND_CLASSES";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTypeCommandMapping.class);

    private static final SyncObject UPDATE_LOCK = new SyncObject("ServiceTypeCommandMappingLock");
    private static Map<ServiceType, Set<Class<Command>>> MAP = new HashMap<>();

    /**
     * Get a set of command classes which can be handled by a service type.
     *
     * @param serviceType the service type for which the command classes are resolved.
     *
     * @return a set of command classes that can be handled by the service type.
     *
     * @throws NotAvailableException if no command classes are available for the service type.
     */
    public static Set<Class<Command>> getCommandClasses(final ServiceType serviceType) throws NotAvailableException {
        synchronized (UPDATE_LOCK) {
            if (MAP.isEmpty()) {
                Registries.getTemplateRegistry().addDataObserver((provider, data) -> buildMap());

                try {
                    if (!Registries.getTemplateRegistry().isDataAvailable()) {
                        Registries.getTemplateRegistry().waitForData();
                    }
                    buildMap();
                } catch (CouldNotPerformException ex) {
                    throw new NotAvailableException("mapping from service types to command classes");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new NotAvailableException("mapping from service types to command classes");
                }
            }

            if (!MAP.containsKey(serviceType) || MAP.get(serviceType).isEmpty()) {
                throw new NotAvailableException("command classes for service type[" + serviceType.name() + "]");
            }

            return MAP.get(serviceType);
        }
    }

    @SuppressWarnings("unchecked")
    private static void buildMap() throws CouldNotPerformException {
        synchronized (UPDATE_LOCK) {
            for (final ServiceTemplate serviceTemplate : Registries.getTemplateRegistry().getServiceTemplates()) {
                MAP.put(serviceTemplate.getType(), new HashSet<>());
                final MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider(serviceTemplate.getType().name() + MetaConfig.class.getSimpleName(), serviceTemplate.getMetaConfig()));

                String commandTypeClasses;
                try {
                    commandTypeClasses = metaConfigPool.getValue(COMMAND_CLASSES_KEY);
                } catch (NotAvailableException e) {
                    continue;
                }

                final String commandTypePackage = OnOffType.class.getPackage().getName();
                for (String commandTypeClass : commandTypeClasses.split(",")) {
                    try {
                        final String className = commandTypePackage + "." + commandTypeClass.trim();
                        Class<Command> commandClass = (Class<Command>) ServiceTypeCommandMapping.class.getClassLoader().loadClass(className);
                        MAP.get(serviceTemplate.getType()).add(commandClass);
                    } catch (ClassNotFoundException ex) {
                        ExceptionPrinter.printHistory("Command class[" + commandTypeClass + "] for service type[" + serviceTemplate.getType().name() + "] not available", ex, LOGGER, LogLevel.WARN);
                    }
                }
            }
        }
    }
}
