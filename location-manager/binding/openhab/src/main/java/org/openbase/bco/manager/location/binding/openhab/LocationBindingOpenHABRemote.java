/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.manager.location.binding.openhab;

/*-
 * #%L
 * BCO Manager Location Binding OpenHAB
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.unit.unitgroup.UnitGroupRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote;
import static org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SEGMENT_DELIMITER;
import org.openbase.jul.extension.openhab.binding.transform.OpenhabCommandTransformer;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.RemoteControllerRegistryImpl;
import rst.domotic.binding.openhab.OpenhabCommandType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author pleminoq
 */
public class LocationBindingOpenHABRemote extends AbstractOpenHABRemote {

    private final RemoteControllerRegistryImpl<String, LocationRemote> locationRegistry;
    private final RemoteControllerRegistryImpl<String, ConnectionRemote> connectionRegistry;
    private final RemoteControllerRegistryImpl<String, UnitGroupRemote> unitGroupRegistry;

    public LocationBindingOpenHABRemote(final boolean hardwareSimulationMode, final RemoteControllerRegistryImpl<String, LocationRemote> locationRegistry, 
            final RemoteControllerRegistryImpl<String, ConnectionRemote> connectionRegistry,
            final RemoteControllerRegistryImpl<String, UnitGroupRemote> unitGroupRegistry) {
        super(hardwareSimulationMode);

        this.locationRegistry = locationRegistry;
        this.connectionRegistry = connectionRegistry;
        this.unitGroupRegistry = unitGroupRegistry;
    }

    @Override
    public void internalReceiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        logger.debug("Ignore update for location manager openhab binding.");
    }

    @Override
    public void internalReceiveCommand(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        //TODO:paramite; compare this to the implementation in the device manager openhab binding
        try {
            RSBRemoteService remote = null;
            if (command.getItem().startsWith("Location")) {
                logger.debug("Received command for location [" + command.getItem() + "] from openhab");
                remote = locationRegistry.get(getIdFromOpenHABCommand(command));
            } else if (command.getItem().startsWith("Connection")) {
                logger.debug("Received command for connection [" + command.getItem() + "] from openhab");
                remote = connectionRegistry.get(getIdFromOpenHABCommand(command));
            } else if (command.getItem().startsWith("UnitGroup")) {
                logger.debug("Received command for unitgroup [" + command.getItem() + "] from openhab");
                remote = unitGroupRegistry.get(getIdFromOpenHABCommand(command));
            }

            if (remote == null) {
                throw new NotAvailableException("No remote for item [" + command.getItem() + "] found");
            }

            Future returnValue;
            ServiceType serviceType = getServiceTypeForCommand(command);
            String methodName = "set" + StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll("Provider", "").replaceAll("Service", "");
            Object serviceData = OpenhabCommandTransformer.getServiceData(command, serviceType);
            
            if(serviceData == null) {
                throw new NotAvailableException("serviceData");
            }
            
            Method relatedMethod;
            try {
                relatedMethod = remote.getClass().getMethod(methodName, serviceData.getClass());
                if (relatedMethod == null) {
                    throw new NotAvailableException(relatedMethod);
                }
            } catch (NoSuchMethodException | SecurityException | NotAvailableException ex) {
                throw new NotAvailableException("Method " + remote + "." + methodName + "(" + serviceData.getClass() + ")", ex);
            }

            try {
                returnValue = (Future) relatedMethod.invoke(remote, serviceData);
            } catch (IllegalAccessException ex) {
                throw new CouldNotPerformException("Cannot access related Method [" + relatedMethod.getName() + "]", ex);
            } catch (IllegalArgumentException ex) {
                throw new CouldNotPerformException("Does not match [" + relatedMethod.getParameterTypes()[0].getName() + "] which is needed by [" + relatedMethod.getName() + "]!", ex);
            } catch (InvocationTargetException ex) {
                throw new CouldNotPerformException("The related method [" + relatedMethod.getName() + "] throws an exception during invocation!", ex);
            }

            GlobalCachedExecutorService.applyErrorHandling(returnValue, (Exception input) -> {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Waiting for result on method failed with exception", input), logger);
                return null;
            }, 30, TimeUnit.SECONDS);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not internal receive command", ex);
        }
    }

    private String getIdFromOpenHABCommand(OpenhabCommandType.OpenhabCommand command) {
        return command.getItemBindingConfig().split(":")[1];
    }

    private ServiceType getServiceTypeForCommand(OpenhabCommandType.OpenhabCommand command) {
        return ServiceType.valueOf(StringProcessor.transformToUpperCase(command.getItem().split(ITEM_SEGMENT_DELIMITER)[1]));
    }
}
