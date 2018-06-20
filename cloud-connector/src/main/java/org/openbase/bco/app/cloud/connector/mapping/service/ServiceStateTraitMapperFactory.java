package org.openbase.bco.app.cloud.connector.mapping.service;

import org.junit.internal.Classes;
import org.openbase.bco.app.cloud.connector.mapping.lib.Trait;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.lang.reflect.InvocationTargetException;

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

    private ServiceStateMapper loadMapper(final ServiceType serviceType, final Trait trait) throws CouldNotPerformException {
        final String serviceTypeComponent = StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replace("State", "");
        final String traitComponent = StringProcessor.transformUpperCaseToCamelCase(trait.name());
        final String className = serviceTypeComponent + traitComponent + MAPPER_CLASS_NAME_ENDING;
        final String fullClassName = getClass().getPackage().getName() + "." + className;

        try {
            return (ServiceStateMapper) Classes.getClass(fullClassName).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not load class for combination of serviceType[" + serviceType.name() + "] and trait[" + trait.name() + "]");
        }
    }
}
