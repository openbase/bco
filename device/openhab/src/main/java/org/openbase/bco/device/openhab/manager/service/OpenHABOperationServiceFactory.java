package org.openbase.bco.device.openhab.manager.service;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.*;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.lang.reflect.InvocationTargetException;

public class OpenHABOperationServiceFactory implements OperationServiceFactory {

    private final static OperationServiceFactory instance = new OpenHABOperationServiceFactory();

    public static OperationServiceFactory getInstance() {
        return instance;
    }

    @Override
    public <UNIT extends UnitController<?, ?>> OperationService newInstance(final ServiceType operationServiceType, final UNIT unit) throws InstantiationException {
        String serviceImplClassName = OpenHABService.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToPascalCase(operationServiceType.name()) + "Impl";
        try {
            final Class<?> serviceImplClass = Class.forName(serviceImplClassName);
            return (OperationService) serviceImplClass.getConstructor(Services.loadOperationServiceClass(operationServiceType)).newInstance(unit);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | java.lang.InstantiationException | InvocationTargetException ex) {
            throw new InstantiationException(OpenHABService.class, ex);
        }
    }
}
