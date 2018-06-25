package org.openbase.bco.app.cloud.connector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
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

import org.junit.internal.Classes;
import org.openbase.bco.app.cloud.connector.mapping.lib.Trait;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTraitMapperFactory {

    private static final String MAPPER_CLASS_NAME_ENDING = "Mapper";

    private static ServiceTraitMapperFactory instance;

    public static synchronized ServiceTraitMapperFactory getInstance() {
        if (instance == null) {
            instance = new ServiceTraitMapperFactory();
        }
        return instance;
    }

    private final Map<String, ServiceTraitMapper> mapperMap;

    private ServiceTraitMapperFactory() {
        this.mapperMap = new HashMap<>();
    }

    public ServiceTraitMapper getServiceStateMapper(final ServiceType serviceType, final Trait trait) throws CouldNotPerformException {
        final String key = StringProcessor.transformUpperCaseToCamelCase(serviceType.name() + "_" + trait.name());
        if (!mapperMap.containsKey(key)) {
            mapperMap.put(key, loadMapper(serviceType, trait));
        }

        return mapperMap.get(key);
    }

    private ServiceTraitMapper loadMapper(final ServiceType serviceType, final Trait trait) throws CouldNotPerformException {
        final String serviceTypeComponent = StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replace("State", "");
        final String traitComponent = StringProcessor.transformUpperCaseToCamelCase(trait.name());
        final String className = serviceTypeComponent + traitComponent + MAPPER_CLASS_NAME_ENDING;
        final String fullClassName = getClass().getPackage().getName() + "." + className;

        try {
            return (ServiceTraitMapper) getClass().getClassLoader().loadClass(fullClassName).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not load class[" + className + "] for combination of serviceType[" + serviceType.name() + "] and trait[" + trait.name() + "]");
        }
    }
}
