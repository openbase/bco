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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class CommandTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandTransformer.class);

    private static final String EMPTY_COMMAND_STRING = "null";

    public static Message getServiceData(final String commandString, final ServiceType serviceType) throws CouldNotPerformException {
        if (commandString.equalsIgnoreCase(EMPTY_COMMAND_STRING)) {
            LOGGER.debug("Ignore state update [" + commandString + "] for service[" + serviceType + "]");
            return null;
        }

        try {
            final ServiceTypeCommandMapping serviceTypeCommandMapping = ServiceTypeCommandMapping.fromServiceType(serviceType);
            Command command = null;
            for (Class<? extends Command> commandClass : serviceTypeCommandMapping.getCommandClasses()) {
                try {
                    command = (Command) commandClass.getMethod("valueOf", commandString.getClass()).invoke(null, commandString);
                    break;
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    LOGGER.error("Command class[" + commandClass.getSimpleName() + "] does not posses a valueOf(String) method", ex);
                } catch (IllegalArgumentException ex) {
                    // continue with the next command class, exception will be thrown if none is found
                }
            }

            if (command == null) {
                throw new CouldNotPerformException("Could not transform [" + commandString + "] into a state for service type[" + serviceType.name() + "]");
            }

            Message serviceData = ServiceStateCommandTransformerPool.getInstance().getTransformer(serviceType, command).transform(command);
            return TimestampProcessor.updateTimestamp(System.currentTimeMillis(), serviceData, TimeUnit.MICROSECONDS);
        } catch (NotAvailableException ex) {
            throw new CouldNotPerformException("Could not transform [" + commandString + "] into a state for service type[" + serviceType.name() + "]", ex);
        }

        //TODO validate that for every servicetype command mapping a transformer exists
    }
}
