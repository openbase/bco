package org.openbase.bco.app.openhab.manager.service;

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

import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.operation.*;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.lang.reflect.InvocationTargetException;

public class OpenHABServiceFactory implements OperationServiceFactory {

    private final static OperationServiceFactory instance = new OpenHABServiceFactory();

    public static OperationServiceFactory getInstance() {
        return instance;
    }

    @Override
    public <UNIT extends Unit> OperationService newInstance(final ServiceType operationServiceType, final UNIT unit) throws InstantiationException {
        String serviceImplClassName = OpenHABService.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(operationServiceType.name()) + "Impl";
        try {
            final Class<?> serviceImplClass = Class.forName(serviceImplClassName);
            return (OperationService) serviceImplClass.getConstructor(unit.getClass()).newInstance(unit);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | java.lang.InstantiationException | InvocationTargetException ex) {
            throw new InstantiationException(OpenHABService.class, ex);
        }
    }

}
