package org.openbase.bco.app.cloud.connector;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import org.openbase.bco.app.cloud.connector.mapping.lib.Command;
import org.openbase.bco.app.cloud.connector.mapping.lib.ErrorCode;
import org.openbase.bco.app.cloud.connector.mapping.lib.Trait;
import org.openbase.bco.app.cloud.connector.mapping.service.ServiceTypeTraitMapping;
import org.openbase.bco.app.cloud.connector.mapping.unit.UnitDataMapper;
import org.openbase.bco.app.cloud.connector.mapping.unit.UnitTypeMapping;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.TimeoutException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.LabelType.Label;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Class parsing JSON requests send by Google and fulfilling them.
 * For the JSON definition have a look at:
 * <ul>
 * <li><a href=https://developers.google.com/actions/smarthome/create-app#actiondevicessync>SYNC</a></li>
 * <li><a href=https://developers.google.com/actions/smarthome/create-app#actiondevicesquery>QUERY</a></li>
 * <li><a href=https://developers.google.com/actions/smarthome/create-app#actiondevicesexecute>EXECUTE</a></li>
 * </ul>
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class FulfillmentHandler {

    //TODO: Exceptions caught and processed in response should be printed by exception printer or logger
    public static final String INTENT_PREFIX = "action.devices.";
    public static final String SYNC_INTENT = INTENT_PREFIX + "SYNC";
    public static final String QUERY_INTENT = INTENT_PREFIX + "QUERY";
    public static final String EXECUTE_INTENT = INTENT_PREFIX + "EXECUTE";

    public static final String INPUTS_KEY = "inputs";
    public static final String INTENT_KEY = "intent";
    public static final String COMMAND_KEY = "command";
    public static final String COMMANDS_KEY = "commands";
    public static final String EXECUTION_KEY = "execution";
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String PAYLOAD_KEY = "payload";
    public static final String PARAMS_KEY = "params";
    public static final String AGENT_USER_ID_KEY = "agentUserId";
    public static final String STATUS_KEY = "status";
    public static final String ID_KEY = "id";
    public static final String IDS_KEY = "ids";
    public static final String TYPE_KEY = "type";
    public static final String TRAITS_KEY = "traits";
    public static final String NAME_KEY = "name";
    public static final String DEVICES_KEY = "devices";
    public static final String ERROR_CODE_KEY = "errorCode";
    public static final String DEBUG_CODE_KEY = "debugCode";

    public static final String EXECUTE_SUCCESS = "SUCCESS";
    public static final String EXECUTE_PENDING = "PENDING";
    public static final String EXECUTE_OFFLINE = "OFFLINE";
    public static final String EXECUTE_ERROR = "ERROR";

    public static final Long REGISTRY_TIMEOUT = 5L;
    public static final Long UNIT_DATA_TIMEOUT = 2L;
    public static final Long UNIT_TASK_TIMEOUT = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(FulfillmentHandler.class);

    /**
     * Handle any smart home request from Google. This is done by parsing the intent
     * and invoking according methods for each.
     *
     * @param request the request from Google parsed into a JsonObject.
     * @return a JsonObject which is the answer for the request
     */
    public JsonObject handleRequest(final JsonObject request) {
        // build basic response type and add payload
        final JsonObject response = new JsonObject();
        final JsonObject payload = new JsonObject();
        response.add(PAYLOAD_KEY, payload);
        try {
            // parse request id
            final String requestId = request.get(REQUEST_ID_KEY).getAsString();
            // add request id to response
            response.addProperty(REQUEST_ID_KEY, requestId);

            // parse input and intent
            final JsonObject input = request.getAsJsonArray(INPUTS_KEY).get(0).getAsJsonObject();
            final String intent = input.get(INTENT_KEY).getAsString();

            // decide what to do based on the intent
            switch (intent) {
                case SYNC_INTENT:
                    LOGGER.info("Handle SYNC");
                    // method will handle a sync intent and fill the payload of the response accordingly
                    handleSync(payload);
                    break;
                case QUERY_INTENT:
                    LOGGER.info("Handle QUERY");
                    // method will handle a query intent and fill the payload of the response accordingly
                    handleQuery(payload, input);
                    break;
                case EXECUTE_INTENT:
                    LOGGER.info("Handle EXECUTE");
                    // method will handle an execute intent and fill the payload of the response accordingly
                    handleExecute(payload, input);
                    break;
                default:
                    setError(payload, "Intent[" + intent + "] is not supported", ErrorCode.NOT_SUPPORTED);
            }
        } catch (IllegalStateException ex) {
            // illegal state is thrown if the json object could not parsed
            // this translates to a protocol error from Google
            setError(payload, ex, ErrorCode.PROTOCOL_ERROR);
        }
        return response;
    }

    /**
     * Handle a sync intent from the Google Assistant.
     * This method will fill the payload object according to this intents specification
     * and the content of the unit registry of BCO.
     *
     * @param payload the payload of the response send to Google.
     */
    public void handleSync(final JsonObject payload) {
        // TODO: debug string and error optional for payload

        // TODO: this has to be an id for a bco instance
        payload.addProperty(AGENT_USER_ID_KEY, CloudConnector.ID);

        final JsonArray devices = new JsonArray();
        try {
            final UnitRegistryRemote unitRegistryRemote = Registries.getUnitRegistry();
            try {
                unitRegistryRemote.waitForData(REGISTRY_TIMEOUT, TimeUnit.SECONDS);
            } catch (CouldNotPerformException | InterruptedException ex) {
                setError(payload, ex, ErrorCode.TIMEOUT);
                return;
            }

            for (final UnitConfig unitConfig : unitRegistryRemote.getUnitConfigs()) {
                final JsonObject device = new JsonObject();

                try {
                    final UnitTypeMapping unitTypeTypeMapping = UnitTypeMapping.getByUnitType(unitConfig.getUnitType());
                    device.addProperty(TYPE_KEY, unitTypeTypeMapping.getDeviceType().getRepresentation());
                } catch (NotAvailableException ex) {
                    LOGGER.warn("Skip unit[" + unitConfig.getAlias(0) + "] because no mapping for unitType[" + unitConfig.getUnitType().name() + "] available");
                    continue;
                }

                device.addProperty(ID_KEY, unitConfig.getId());

                device.addProperty("willReportState", false); // This could be activated in the future
                device.addProperty("roomHint", LabelProcessor.getFirstLabel(unitRegistryRemote.getUnitConfigById(unitConfig.getPlacementConfig().getLocationId()).getLabel()));

                final JsonObject attributes = new JsonObject();
                final JsonArray traits = new JsonArray();
                final Set<ServiceType> serviceTypeSet = new HashSet<>();
                for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    final ServiceType serviceType = serviceConfig.getServiceDescription().getServiceType();
                    if (serviceTypeSet.contains(serviceType)) {
                        continue;
                    }
                    serviceTypeSet.add(serviceType);

                    try {
                        final ServiceTypeTraitMapping serviceTypeTraitMapping = ServiceTypeTraitMapping.getByServiceType(serviceType);
                        for (final Trait trait : serviceTypeTraitMapping.getTraitSet()) {
                            traits.add(trait.getRepresentation());
                            trait.getServiceStateMapper().addAttributes(unitConfig, attributes);
                        }
                    } catch (NotAvailableException ex) {
                        LOGGER.warn("Skip serviceType[" + serviceType.name() + "] for unit[" + unitConfig.getAlias(0) + "] because no trait mapping available");
                    }
                }

                if (traits.size() == 0) {
                    // skip because no traits have been added
                    LOGGER.warn("Skip unit[" + unitConfig.getAlias(0) + "] because no traits could be added");
                    continue;
                }
                device.add(TRAITS_KEY, traits);
                device.add("attributes", attributes);

                final JsonObject name = new JsonObject();
                name.addProperty("name", LabelProcessor.getFirstLabel(unitConfig.getLabel()));
                final JsonArray defaultNames = new JsonArray();
                for (final String alias : unitConfig.getAliasList()) {
                    defaultNames.add(alias.replace("-", " "));
                }
                name.add("defaultNames", defaultNames);

                final JsonArray nickNames = new JsonArray();
                for (final Label.MapFieldEntry mapFieldEntry : unitConfig.getLabel().getEntryList()) {
                    for (final String label : mapFieldEntry.getValueList()) {
                        nickNames.add(label);
                    }
                }
                name.add("nicknames", nickNames);

                device.add(NAME_KEY, name);

                final JsonObject deviceInfo = new JsonObject();
//                deviceInfo.addProperty("manufacturer", Registries.getClassRegistry(true).getDeviceClassById(Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId()).getDeviceConfig().getDeviceClassId()).getCompany());
                //TODO: missing: model, hwVersion, swVersion
                device.add("deviceInfo", deviceInfo);

                //TODO: custom data missing
                devices.add(device);
            }
            payload.add(DEVICES_KEY, devices);
        } catch (CouldNotPerformException ex) {
            setError(payload, ex, ErrorCode.UNKNOWN_ERROR);
        }
    }

    /**
     * Handle a query intent for the Google Assistant.
     * This method fills the payload object according to the given input.
     *
     * @param payload the payload of the response
     * @param input   the input object as send by Google
     */
    @SuppressWarnings("unchecked")
    private void handleQuery(final JsonObject payload, final JsonObject input) {
        // parse device ids from request input
        final List<String> deviceIdList = new ArrayList<>();
        for (final JsonElement elem : input.getAsJsonObject(PAYLOAD_KEY).getAsJsonArray(DEVICES_KEY)) {
            deviceIdList.add(elem.getAsJsonObject().get(ID_KEY).getAsString());
        }

        // create response
        JsonObject devices = new JsonObject();
        payload.add(DEVICES_KEY, devices);

        // iterate over all requested devices and add their states to the response
        final Map<String, Future> idFutureMap = new HashMap<>();
        for (final String id : deviceIdList) {
            // add a deviceState for the device
            final JsonObject deviceState = new JsonObject();
            devices.add(id, deviceState);

            // create a task which will fill the device state accordingly
            // this is a separate task because it will be waited for unit data and this should be done for all requested in parallel
            final Future future = GlobalCachedExecutorService.submit((Callable<Void>) () -> {
                try {
                    // get unit remote by id
                    final UnitRemote unitRemote = Units.getUnit(id, false);

                    try {
                        // wait for data
                        unitRemote.waitForData(UNIT_DATA_TIMEOUT, TimeUnit.SECONDS);
                    } catch (CouldNotPerformException ex) {
                        // this is thrown on a timeout so assume the unit is not online
                        deviceState.addProperty(DEBUG_CODE_KEY, ex.getMessage());
                        deviceState.addProperty("online", false);
                        return null;
                    }
                    // received data so unit is online
                    deviceState.addProperty("online", true);

                    // map state of unit remote into device state object
                    UnitDataMapper.getByType(unitRemote.getUnitType()).map(unitRemote, deviceState);
                } catch (NotAvailableException ex) {
                    // thrown if the unit remote is not available so propagate deviceNotFound error
                    setError(deviceState, ex, ErrorCode.DEVICE_NOT_FOUND);
                } catch (InterruptedException ex) {
                    // interrupted so propagate a timeout error
                    setError(deviceState, ex, ErrorCode.TIMEOUT);
                }

                return null;
            });

            // save future mapped to id
            idFutureMap.put(id, future);
        }

        // wait for all futures
        try {
            for (final Entry<String, Future> entry : idFutureMap.entrySet()) {
                try {
                    entry.getValue().get();
                } catch (ExecutionException ex) {
                    // this should not happen since the task of the future should handle all errors internally
                    final JsonObject deviceState = devices.getAsJsonObject(entry.getKey());
                    setError(deviceState, ex, ErrorCode.UNKNOWN_ERROR);
                }
            }
        } catch (InterruptedException ex) {
            // interrupted so cancel all futures, then they will add an according error
            idFutureMap.values().forEach(future -> {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            });
            // restore interrupt
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handle an execute intent from the Google Assistant.
     * This method will fill the payload object according to this intents specification
     * and the received input.
     *
     * @param payload the payload of the response send to Google.
     * @param input   the input object as send by Google
     */
    private void handleExecute(final JsonObject payload, final JsonObject input) {
        // create a map which has the unit id as key and a list of commands to execute as value
        final Map<String, List<JsonObject>> idCommandMap = new HashMap<>();
        // iterate over all commands
        for (JsonElement commandElem : input.getAsJsonObject(PAYLOAD_KEY).getAsJsonArray(COMMANDS_KEY)) {
            // treat command as json object
            final JsonObject command = commandElem.getAsJsonObject();

            // convert all execution objects into the command into a list
            List<JsonObject> executionList = new ArrayList<>();
            for (JsonElement executeElem : command.getAsJsonArray(EXECUTION_KEY)) {
                executionList.add(executeElem.getAsJsonObject());
            }

            // iterate over all devices this command is for
            for (JsonElement deviceElem : command.getAsJsonArray(DEVICES_KEY)) {
                // extract unit id
                final String id = deviceElem.getAsJsonObject().get(ID_KEY).getAsString();

                // add execution list to an already existing list or create a new entry for it
                if (!idCommandMap.containsKey(id)) {
                    idCommandMap.put(id, new ArrayList<>(executionList));
                } else {
                    idCommandMap.get(id).addAll(executionList);
                }
            }
        }

        // create basic response type
        final JsonArray commands = new JsonArray();
        payload.add(COMMANDS_KEY, commands);

        final Map<String, Future> idFutureMap = new HashMap<>();
        for (final Entry<String, List<JsonObject>> idCommand : idCommandMap.entrySet()) {
            try {
                final UnitRemote unitRemote = Units.getUnit(idCommand.getKey(), false);

                final Future future = GlobalCachedExecutorService.submit((Callable<Void>) () -> {
                    try {
                        // wait for data
                        unitRemote.waitForData(UNIT_DATA_TIMEOUT, TimeUnit.SECONDS);
                    } catch (CouldNotPerformException ex) {
                        // throw timeout so that thread waiting for this tasks knows that waiting for data failed
                        throw new TimeoutException();
                    } catch (InterruptedException ex) {
                        // interrupted so just return to finish this task
                        return null;
                    }

                    // iterate over all executions
                    for (final JsonObject execution : idCommand.getValue()) {
                        // extract command name and param object
                        final String commandName = execution.get(COMMAND_KEY).getAsString();
                        final JsonObject params = execution.getAsJsonObject(PARAMS_KEY);

                        // find command type by name
                        final Command commandType = Command.getByRepresentation(commandName);
                        // find trait by command type and params
                        final Trait trait = Trait.getByCommand(commandType, params);
                        //TODO: mapping for trait and service type is not possible
                        // get service type for trait
                        final ServiceType serviceType = trait.getServiceStateMapper().getServiceType();
                        // parse trait param into service state
                        final Message serviceState = trait.getServiceStateMapper().map(params, commandType);
                        // invoke setter for service type on remote
                        final Future serviceFuture = (Future)
                                Services.invokeOperationServiceMethod(serviceType, unitRemote, serviceState);

                        // wait for result
                        try {
                            serviceFuture.get(UNIT_TASK_TIMEOUT, TimeUnit.SECONDS);
                        } catch (InterruptedException ex) {
                            // cancel internal task and finish normally
                            serviceFuture.cancel(true);
                        } catch (ExecutionException ex) {
                            // throw exception to inform task waiting for this one
                            throw new CouldNotPerformException("Invoking service[" + serviceType.name() + "] " +
                                    "operation method failed for unit[" + unitRemote + "]", ex);
                        }
                    }

                    return null;
                });

                // add future to a map
                idFutureMap.put(idCommand.getKey(), future);
            } catch (NotAvailableException ex) {
                // thrown if the unit remote is not available so propagate deviceNotFound error
                setError(payload, ex, ErrorCode.DEVICE_NOT_FOUND);
            } catch (InterruptedException ex) {
                // interrupted so propagate a timeout error
                setError(payload, ex, ErrorCode.TIMEOUT);
            }
        }

        // create sets of ids for different finishing states: success, offline, error
        Set<String> success = new HashSet<>();
        Set<String> pending = new HashSet<>();
        Set<String> offline = new HashSet<>();
        Set<String> error = new HashSet<>();
        try {
            // iterate and wait for every task
            for (Entry<String, Future> entry : idFutureMap.entrySet()) {
                try {
                    // wait without timeout because internal tasks should only be able to run for a limited amount of time
                    entry.getValue().get();
                    // task was executed successfully so add the id of the unit to the successes
                    success.add(entry.getKey());
                } catch (ExecutionException ex) {
                    // task execution failed
                    if (ex.getCause() instanceof TimeoutException) {
                        // cause was a timeout meaning waiting for data failed, so assume that the unit is offline
                        offline.add(entry.getKey());
                    } else if (ex.getCause() instanceof java.util.concurrent.TimeoutException) {
                        // cause was a timeout on waiting for operation service return so still pending
                        pending.add(entry.getKey());
                    } else {
                        // something failed so add as error
                        error.add(entry.getKey());
                        // print failure reason
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not perform execute commands for unit[" + entry.getKey() + "]", ex), LOGGER);
                    }
                }
            }
        } catch (InterruptedException ex) {
            // interrupted so cancel all futures, then they will add an according error
            idFutureMap.values().forEach(future -> {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            });
            // restore interrupt
            Thread.currentThread().interrupt();
        }

        // add results to response
        addExecuteResults(commands, success, EXECUTE_SUCCESS);
        addExecuteResults(commands, pending, EXECUTE_PENDING);
        addExecuteResults(commands, offline, EXECUTE_OFFLINE);
        addExecuteResults(commands, error, EXECUTE_ERROR);
    }

    /**
     * Convert an id set into a JsonArray and add it as a JsonObject to commands while also setting
     * the status to result type.
     * This is just a helper method which needs to be performed four times for an execute request
     * and therefore is outsourced to its own method.
     *
     * @param commands   the JsonArray to which a JsonObject will be added if the id set is not empty
     * @param idSet      the id set included in the JsonObject added to commands
     * @param resultType the result type which is the status of the command
     */
    private void addExecuteResults(final JsonArray commands, final Set<String> idSet, final String resultType) {
        if (idSet.isEmpty()) {
            return;
        }

        JsonObject command = new JsonObject();
        JsonArray idArray = new JsonArray();
        for (final String id : idSet) {
            idArray.add(id);
        }
        command.add(IDS_KEY, idArray);
        command.addProperty(STATUS_KEY, resultType);

        commands.add(command);
    }

    public static void setError(final JsonObject jsonObject, final Exception exception, final ErrorCode errorCode) {
        setError(jsonObject, exception.toString(), errorCode);

        ExceptionPrinter.printHistory(exception, LOGGER);
    }

    private static void setError(final JsonObject jsonObject, final String debugString, final ErrorCode errorCode) {
        jsonObject.addProperty(ERROR_CODE_KEY, errorCode.toString());
        jsonObject.addProperty(DEBUG_CODE_KEY, debugString);
    }
}
