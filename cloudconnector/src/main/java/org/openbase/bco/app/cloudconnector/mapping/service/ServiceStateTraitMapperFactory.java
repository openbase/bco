package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import org.openbase.bco.app.cloudconnector.mapping.lib.Trait;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.service.ServiceCommunicationTypeType.ServiceCommunicationType.CommunicationType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceStateTraitMapperFactory {

    private static final String MAPPER_CLASS_NAME_ENDING = "Mapper";

    private static ServiceStateTraitMapperFactory instance;

    public static synchronized ServiceStateTraitMapperFactory getInstance() {
        if (instance == null) {
            instance = new ServiceStateTraitMapperFactory();
        }
        return instance;
    }

    private final Map<String, ServiceStateTraitMapper> mapperMap;

    private ServiceStateTraitMapperFactory() {
        this.mapperMap = new HashMap<>();
    }

    public ServiceStateTraitMapper getServiceStateMapper(final ServiceType serviceType, final Trait trait) throws CouldNotPerformException {
        final CommunicationType communicationType = Registries.getTemplateRegistry().getServiceTemplateByType(serviceType).getCommunicationType();
        final String key = StringProcessor.transformUpperCaseToPascalCase(communicationType.name() + "_" + trait.name());
        if (!mapperMap.containsKey(key)) {
            mapperMap.put(key, loadMapper(communicationType, trait));
        }

        return mapperMap.get(key);
    }

    private ServiceStateTraitMapper loadMapper(final CommunicationType communicationType, final Trait trait) throws CouldNotPerformException {
        final String serviceTypeComponent = StringProcessor.transformUpperCaseToPascalCase(communicationType.name());
        final String traitComponent = StringProcessor.transformUpperCaseToPascalCase(trait.name());
        final String className = serviceTypeComponent + traitComponent + MAPPER_CLASS_NAME_ENDING;
        final String fullClassName = getClass().getPackage().getName() + "." + className;

        try {
            return (ServiceStateTraitMapper) getClass().getClassLoader().loadClass(fullClassName).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not load class[" + className + "] for combination of serviceType[" + communicationType.name() + "] and trait[" + trait.name() + "]");
        }
    }
}
