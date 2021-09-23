package org.openbase.bco.device.openhab.manager;

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.bco.device.openhab.manager.transform.ServiceStateCommandTransformerPool;
import org.openbase.bco.device.openhab.manager.transform.ServiceTypeCommandMapping;
import org.openbase.bco.device.openhab.registry.synchronizer.OpenHABItemProcessor;
import org.openbase.bco.device.openhab.registry.synchronizer.OpenHABItemProcessor.OpenHABItemNameMetaData;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class CommandExecutor implements Observer<Object, JsonObject> {

    public static final String PAYLOAD_KEY = "payload";
    public static final String PAYLOAD_STATE_KEY = "value";
    public static final String PAYLOAD_STATE_TYPE_KEY = "type";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

    private final UnitControllerRegistry<UnitController<?, ?>> unitControllerRegistry;
    private final JsonParser jsonParser;

    public CommandExecutor(final UnitControllerRegistry unitControllerRegistry) {
        this.unitControllerRegistry = unitControllerRegistry;
        this.jsonParser = new JsonParser();
    }

    @Override
    public void update(Object source, JsonObject payload) {

        //System.out.println("payload: " + payload.toString());

        // extract item name from topic
        final String topic = payload.get(OpenHABRestCommunicator.TOPIC_KEY).getAsString();

        // topic structure: smarthome/items/{itemName}/command
        final String itemName = topic.split(OpenHABRestCommunicator.TOPIC_SEPARATOR)[2];

        // extract payload
        final JsonObject payloadObject = jsonParser.parse(payload.get(PAYLOAD_KEY).getAsString()).getAsJsonObject();
        final String state = payloadObject.get(PAYLOAD_STATE_KEY).getAsString();
        final String type = payloadObject.get(PAYLOAD_STATE_TYPE_KEY).getAsString();

        try {
            applyStateUpdate(itemName, type, state);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not apply state update[" + state + "] for item[" + itemName + "]", ex, LOGGER, LogLevel.WARN);
        }
    }

    /**
     * Call {@link #applyStateUpdate(String, String, String, boolean)} with false.
     *
     * @param itemName  the item identifying the unit whose the state should be updated.
     * @param stateType a string serializing the state to be set.
     * @throws CouldNotPerformException if applying the state update fails.
     */
    public void applyStateUpdate(final String itemName, final String stateType, final String state) throws CouldNotPerformException {
        applyStateUpdate(itemName, stateType, state, false);
    }

    /**
     * Retrieve a unit controller by an item name and apply an update according to the serialized state.
     * If the update is the result of a system sync it will be scheduled shortly so that the state is only applied.
     * Else the update is handled as a human action.
     *
     * @param itemName   the item identifying the unit whose the state should be updated.
     * @param stateType  defines the type class.
     * @param state      a string serializing the state to be set.
     * @param systemSync flag determining if the state update is the result of a system sync.
     * @throws CouldNotPerformException
     */
    public void applyStateUpdate(final String itemName, final String stateType, final String state, final boolean systemSync) throws CouldNotPerformException {

        // filter empty events
        if (state == null || state.equalsIgnoreCase(EMPTY_COMMAND_STRING)) {
            return;
        }

        OpenHABItemNameMetaData metaData;
        try {
            metaData = OpenHABItemProcessor.getMetaData(itemName);
        } catch (CouldNotPerformException ex) {
            // skip update for non bco handled items
            return;
        }
        try {
            // load controller
            final String controllerId = Registries.getUnitRegistry().getUnitConfigByAlias(metaData.getAlias()).getId();

            // filter all events that are not handled by this instance.
            if (!unitControllerRegistry.contains(controllerId)) {
                return;
            }

            final UnitController<?, ?> unitController = unitControllerRegistry.get(controllerId);

            Message.Builder serviceStateBuilder = getServiceData(stateType, state, metaData.getServiceType()).toBuilder();

            // update the responsible action to show that it was triggered by openHAB and add other parameters
            // note that the responsible action is overwritten if it matches a requested state in the unit controller and thus was triggered by a different user through BCO
            if (systemSync) {
                serviceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, metaData.getServiceType(), unitController, Timeout.INFINITY_TIMEOUT, TimeUnit.MINUTES, false, false, false, Priority.NO, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.SYSTEM).build());
            } else {
                serviceStateBuilder = ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, metaData.getServiceType(), unitController, 30, TimeUnit.MINUTES, false, false, true, Priority.HIGH, ActionInitiator.newBuilder().setInitiatorType(InitiatorType.HUMAN).build());
            }

            LOGGER.info("Apply ItemUpdate[" + itemName + "=" + state + "].");
            unitController.applyDataUpdate(serviceStateBuilder, metaData.getServiceType());

        } catch (InvalidStateException ex) {
            LOGGER.debug("Ignore state update [" + state + "] for service[" + metaData.getServiceType() + "]", ex);
        } catch (CouldNotPerformException ex) {
            LOGGER.warn("Ignore state update [" + state + "] for service[" + metaData.getServiceType() + "]", ex);
        }
    }

    private static final String EMPTY_COMMAND_STRING = "null";

    public static Message getServiceData(final String stateType, final String commandString, final ServiceType serviceType) throws CouldNotPerformException {

        try {
            Command command = null;
            MultiException.ExceptionStack exceptionStack = null;

            Class<? extends Command> commandClass = ServiceTypeCommandMapping.lookupCommandClass(stateType);
            try {
                command = (Command) commandClass.getMethod("valueOf", commandString.getClass()).invoke(null, commandString);
            } catch (IllegalAccessException | NoSuchMethodException ex) {
                exceptionStack = MultiException.push(CommandExecutor.class, new InvalidStateException("Command class[" + commandClass.getSimpleName() + "] does not posses a valueOf(String) method", ex), exceptionStack);
            } catch (IllegalArgumentException ex) {
                // continue with the next command class, exception will be thrown if none is found
                exceptionStack = MultiException.push(CommandExecutor.class, ex, exceptionStack);
            } catch (InvocationTargetException ex) {
                // ignore because the value of method threw an exception, this can happen if e.g. 0 is returned for
                // a roller shutter as the opening ratio and the stopMoveType is tested

                exceptionStack = MultiException.push(CommandExecutor.class, ex, exceptionStack);

                //apply workaround for temperature values
                // todo: implement valueOf() method in all transformer and individually parse the string and setup the physical unit as well
                try {
                    command = (Command) commandClass.getMethod("valueOf", commandString.getClass()).invoke(null, commandString
                            .replace(" °C", "")
                            .replace(" %", "")
                    );
                } catch (Exception exx) {
                    exceptionStack = MultiException.push(CommandExecutor.class, exx, exceptionStack);
                }
            }

            if (command == null) {

                if (exceptionStack == null) {
                    exceptionStack = MultiException.push(CommandExecutor.class, new InvalidStateException("Command class not available! Please configure the eclipse smart home command class within the meta config of service template of type " + serviceType.name()), exceptionStack);
                }

                MultiException.checkAndThrow(() -> "Could not transform [" + commandString + "] into a state for service type[" + serviceType.name() + "]", exceptionStack);
            }

            Message serviceData = ServiceStateCommandTransformerPool.getInstance().getTransformer(serviceType, command.getClass()).transform(command);
            return TimestampProcessor.updateTimestamp(System.currentTimeMillis(), serviceData, TimeUnit.MILLISECONDS);
        } catch (NotAvailableException ex) {
            throw new CouldNotPerformException("Could not transform [" + commandString + "] of class [" + commandString.getClass().getSimpleName() + "] into a state for service type[" + serviceType.name() + "]", ex);
        }
    }
}
