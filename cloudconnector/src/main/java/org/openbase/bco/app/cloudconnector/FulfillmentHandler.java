package org.openbase.bco.app.cloudconnector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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
import com.google.protobuf.Message;
import org.openbase.bco.app.cloudconnector.mapping.lib.Command;
import org.openbase.bco.app.cloudconnector.mapping.lib.ErrorCode;
import org.openbase.bco.app.cloudconnector.mapping.lib.Trait;
import org.openbase.bco.app.cloudconnector.mapping.service.ServiceStateTraitMapper;
import org.openbase.bco.app.cloudconnector.mapping.service.ServiceStateTraitMapperFactory;
import org.openbase.bco.app.cloudconnector.mapping.unit.UnitDataMapper;
import org.openbase.bco.app.cloudconnector.mapping.unit.UnitTypeMapping;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.MultiException.ExceptionStack;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.openbase.type.language.LabelType.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

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

    public static final String INTENT_PREFIX = "action.devices.";
    public static final String SYNC_INTENT = INTENT_PREFIX + "SYNC";
    public static final String QUERY_INTENT = INTENT_PREFIX + "QUERY";
    public static final String EXECUTE_INTENT = INTENT_PREFIX + "EXECUTE";
    public static final String DISCONNECT_INTENT = INTENT_PREFIX + "DISCONNECT";

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
     *
     * @return a JsonObject which is the answer for the request
     */
    public static JsonObject handleRequest(final JsonObject request, final String userId, final String authenticationToken, final String authorizationToken) {
        // build basic response type and add payload
        final JsonObject response = new JsonObject();
        final JsonObject payload = new JsonObject();
        try {
            // parse input and intent
            final JsonObject input = request.getAsJsonArray(INPUTS_KEY).get(0).getAsJsonObject();
            final String intent = input.get(INTENT_KEY).getAsString();

            // decide what to do based on the intent
            switch (intent) {
                case SYNC_INTENT:
                    // method will handle a sync intent and fill the payload of the response accordingly
                    handleSync(payload, userId);
                    break;
                case QUERY_INTENT:
                    // method will handle a query intent and fill the payload of the response accordingly
                    handleQuery(payload, input);
                    break;
                case EXECUTE_INTENT:
                    // method will handle an execute intent and fill the payload of the response accordingly
                    handleExecute(payload, input, authenticationToken, authorizationToken);
                    break;
                case DISCONNECT_INTENT:
                    // TODO: what needs to be handled? -> do not attempt syncs when disconnected?
                    // when disconnecting google expects an empty response so return that
                    return response;
                default:
                    setError(payload, "Intent[" + intent + "] is not supported", ErrorCode.NOT_SUPPORTED);
            }
        } catch (IllegalStateException ex) {
            // illegal state is thrown if the json object could not parsed
            // this translates to a protocol error from Google
            setError(payload, ex, ErrorCode.PROTOCOL_ERROR);
        }
        // parse request id
        final String requestId = request.get(REQUEST_ID_KEY).getAsString();
        // add request id to response
        response.addProperty(REQUEST_ID_KEY, requestId);
        // add payload to response
        response.add(PAYLOAD_KEY, payload);
        return response;
    }

    private static Set<UnitConfig> getUnitConfigsHandledByDevice(final UnitConfig deviceUnitConfig) throws CouldNotPerformException {
        final Set<UnitConfig> handledByDevice = new HashSet<>();
        for (final String hostedUnitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
            final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(hostedUnitId);

            if (unitConfig.getBoundToUnitHost() && unitConfig.getLabel().equals(deviceUnitConfig.getLabel())) {
                handledByDevice.add(unitConfig);
            }
        }

        return handledByDevice;
    }

    /**
     * Handle a sync intent from the Google Assistant.
     * This method will fill the payload object according to this intents specification
     * and the content of the unit registry of BCO.
     *
     * @param payload the payload of the response send to Google.
     */
    public static void handleSync(final JsonObject payload, final String userId) {
        final JsonArray devices = new JsonArray();
        try {
            final UnitRegistryRemote unitRegistryRemote = Registries.getUnitRegistry();
            try {
                unitRegistryRemote.waitForData(REGISTRY_TIMEOUT, TimeUnit.SECONDS);
                Registries.getTemplateRegistry().waitForData(REGISTRY_TIMEOUT, TimeUnit.SECONDS);
            } catch (CouldNotPerformException ex) {
                setError(payload, ex, ErrorCode.TIMEOUT);
                return;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                setError(payload, ex, ErrorCode.DEVICE_OFFLINE);
                return;
            }
            payload.addProperty(AGENT_USER_ID_KEY, userId + "@" + Registries.getUnitRegistry().getUnitConfigByAlias(UnitRegistry.BCO_USER_ALIAS).getId());

            final Set<String> handledUnitConfigs = new HashSet<>();

            // if a unit config is bound to and hosted by a device and this device hosts more than one unit
            // with the same label which are all bound to it, register them as one
            for (final UnitConfig deviceUnitConfig : unitRegistryRemote.getUnitConfigsByUnitType(UnitType.DEVICE)) {
                // ignore disabled devices
                if (deviceUnitConfig.getEnablingState().getValue() == State.DISABLED) {
                    continue;
                }

                if (deviceUnitConfig.getDeviceConfig().getUnitIdCount() < 2) {
                    continue;
                }

                final Map<UnitConfig, UnitTypeMapping> unitConfigTypeMapping = new HashMap<>();
                for (final UnitConfig hostedUnit : getUnitConfigsHandledByDevice(deviceUnitConfig)) {
                    try {
                        final UnitTypeMapping unitTypeMapping = UnitTypeMapping.getByUnitType(hostedUnit.getUnitType());
                        unitConfigTypeMapping.put(hostedUnit, unitTypeMapping);
                    } catch (NotAvailableException ex) {
                        // print warning
                        LOGGER.warn("Skip unit[" + hostedUnit.getAlias(0) + "]: " + ex.getMessage());
                        // unit does not need to be handled later because no type mapping exists
                        handledUnitConfigs.add(hostedUnit.getId());
                    }
                }

                // test if there are at least to units hosted by this devices which are bound and have the same label
                if (unitConfigTypeMapping.size() > 1) {
                    try {
                        // create google device for this device with all its internal units
                        devices.add(createJsonDevice(deviceUnitConfig, unitConfigTypeMapping, userId));
                        // add all units already handled by this routine
                        for (final UnitConfig unitConfig : unitConfigTypeMapping.keySet()) {
                            handledUnitConfigs.add(unitConfig.getId());
                        }
                    } catch (NotAvailableException ex) {
                        LOGGER.warn("Skip device[" + deviceUnitConfig.getAlias(0) + "]: " + ex.getMessage());
                    }
                }
            }

            // register unit groups separately because they depend on the unit type of their group configuration
            for (final UnitConfig unitGroup : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.UNIT_GROUP)) {
                // ignore disabled groups
                if (unitGroup.getEnablingState().getValue() == State.DISABLED) {
                    continue;
                }

                final UnitType unitType = unitGroup.getUnitGroupConfig().getUnitType();
                if (unitType == UnitType.UNKNOWN) {
                    //TODO: this could possibly be handled by a mapping of services to traits and choosing a random device
                    LOGGER.warn("Skip unit group[" + unitGroup.getAlias(0) + "] because unit type is unknown");
                } else {
                    try {
                        final UnitTypeMapping unitTypeMapping = UnitTypeMapping.getByUnitType(unitGroup.getUnitGroupConfig().getUnitType());
                        devices.add(createJsonDevice(unitGroup, unitTypeMapping, userId));
                    } catch (NotAvailableException ex) {
                        LOGGER.warn("Skip unit group[" + unitGroup.getAlias(0) + "]: " + ex.getMessage());
                    }
                }
            }

            for (final UnitConfig unitConfig : unitRegistryRemote.getUnitConfigs()) {
                // ignore disabled units
                if (unitConfig.getEnablingState().getValue() == State.DISABLED) {
                    continue;
                }

                switch (unitConfig.getUnitType()) {
                    case LOCATION:
                    case DEVICE:
                    case GATEWAY:
                    case USER:
                    case AUTHORIZATION_GROUP:
                    case OBJECT:
                    case UNIT_GROUP:
                        // skip locations, devices, user ...
                        // skip unit groups because they are handled above
                        continue;
                }

                if (handledUnitConfigs.contains(unitConfig.getId())) {
                    // unit already handled by device routine above
                    continue;
                }

                try {
                    final UnitTypeMapping unitTypeMapping = UnitTypeMapping.getByUnitType(unitConfig.getUnitType());
                    devices.add(createJsonDevice(unitConfig, unitTypeMapping, userId));
                } catch (NotAvailableException ex) {
                    LOGGER.warn("Skip unit[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "]: " + ex.getMessage());
                }
            }
            payload.add(DEVICES_KEY, devices);
        } catch (CouldNotPerformException ex) {
            setError(payload, ex, ErrorCode.UNKNOWN_ERROR);
        }
    }

    private static JsonObject createJsonDevice(final UnitConfig unitConfig, final UnitTypeMapping unitTypeMapping, final String userId) throws CouldNotPerformException {
        final Map<UnitConfig, UnitTypeMapping> unitConfigTypeMapping = new HashMap<>();
        unitConfigTypeMapping.put(unitConfig, unitTypeMapping);
        return createJsonDevice(unitConfig, unitConfigTypeMapping, userId);
    }

    private static JsonObject createJsonDevice(final UnitConfig host, final Map<UnitConfig, UnitTypeMapping> mappings, final String userId) throws CouldNotPerformException {
        final JsonObject device = new JsonObject();

        // resolve locale of user to use preferred label
        final Locale userLocale = new Locale(Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig().getLanguage());

        device.addProperty(ID_KEY, host.getId());
        device.addProperty("willReportState", false); // This could be activated in the future

        // always register units at a tile so that they will respond to that when queried like this by google
        UnitConfig location = Registries.getUnitRegistry().getUnitConfigById(host.getPlacementConfig().getLocationId());
        while (location.getLocationConfig().getLocationType() == LocationType.REGION) {
            location = Registries.getUnitRegistry().getUnitConfigById(location.getPlacementConfig().getLocationId());
        }
        device.addProperty("roomHint", LabelProcessor.getBestMatch(userLocale, location.getLabel()));

        final JsonObject name = new JsonObject();
        final String mainName = StringProcessor.insertSpaceBetweenPascalCase(LabelProcessor.getBestMatch(userLocale, host.getLabel()));
        name.addProperty("name", mainName);
        final JsonArray defaultNames = new JsonArray();
        for (final String alias : host.getAliasList()) {
            defaultNames.add(StringProcessor.insertSpaceBetweenPascalCase(alias.replace("-", " ")));
        }
        name.add("defaultNames", defaultNames);

        //TODO: remove this if case
        if (!mainName.equalsIgnoreCase("leselampe")) {
            final JsonArray nickNames = new JsonArray();
            for (final Label.MapFieldEntry mapFieldEntry : host.getLabel().getEntryList()) {
                for (final String label : mapFieldEntry.getValueList()) {
                    final String nickName = StringProcessor.insertSpaceBetweenPascalCase(label);
                    if (nickName.equals(mainName)) {
                        continue;
                    }

                    nickNames.add(StringProcessor.insertSpaceBetweenPascalCase(label));
                }
            }
            if (nickNames.size() != 0) {
                name.add("nicknames", nickNames);
            }
        }
        device.add(NAME_KEY, name);

        if (mappings.isEmpty()) {
            throw new NotAvailableException("UnitTypeMappings for host[" + host.getAliasList().get(0) + "]");
        }
        //TODO: can this be solved differently
        // use the first unit type mapping to resolve the device type
        device.addProperty(TYPE_KEY, mappings.values().iterator().next().getDeviceType().getRepresentation());

        final JsonObject attributes = new JsonObject();
        final JsonArray traits = new JsonArray();
        for (final Entry<UnitConfig, UnitTypeMapping> entry : mappings.entrySet()) {
            for (final Trait trait : entry.getValue().getTraitSet()) {
                switch (trait) {
                    case MODES:
                    case TOGGLES:
                        // skip modes and toggles trait because they are currently not really supported by google
                        continue;
                }
                final ServiceType serviceType = entry.getValue().getServiceType(trait);
                traits.add(trait.getRepresentation());
                try {
                    ServiceStateTraitMapperFactory.getInstance().getServiceStateMapper(serviceType, trait).addAttributes(entry.getKey(), attributes);
                } catch (CouldNotPerformException ex) {
                    LOGGER.warn("Skip trait[" + trait.name() + "] serviceType[" + serviceType.name() + "]: " + ex.getMessage());
                }
            }
        }


        if (traits.size() == 0) {
            // skip because no traits have been added
            throw new NotAvailableException("Traits for unit[" + host.getAlias(0) + "]");
        }
        device.add(TRAITS_KEY, traits);
        device.add("attributes", attributes);

        final JsonObject deviceInfo = new JsonObject();
//                deviceInfo.addProperty("manufacturer", Registries.getClassRegistry(true).getDeviceClassById(Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId()).getDeviceConfig().getDeviceClassId()).getCompany());
        //TODO: missing: model, hwVersion, swVersion
        device.add("deviceInfo", deviceInfo);

        //TODO: custom data missing
        return device;
    }

    /**
     * Handle a query intent for the Google Assistant.
     * This method fills the payload object according to the given input.
     *
     * @param payload the payload of the response
     * @param input   the input object as send by Google
     */
    private static void handleQuery(final JsonObject payload, final JsonObject input) {
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

            // create a tasks which will fill the device state accordingly
            // this is done in separate tasks because it will be waited for unit data and this should be done for all requested in parallel
            try {
                final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(id);
                final SyncObject syncObject = new SyncObject(UnitConfigProcessor.getDefaultAlias(unitConfig, "?"));
                if (unitConfig.getUnitType() == UnitType.DEVICE) {
                    // if unit is a device start tasks for all its internal dal units
                    final Future future = GlobalCachedExecutorService.submit(() -> {
                        final Set<Future> internalFutureSet = new HashSet<>();
                        // start task for all internal units
                        for (final UnitConfig hostedUnit : getUnitConfigsHandledByDevice(unitConfig)) {
                            internalFutureSet.add(createQueryTask(hostedUnit, deviceState, syncObject));
                        }

                        // wait for them to finish
                        for (final Future internalFuture : internalFutureSet) {
                            internalFuture.get();
                        }
                        return null;
                    });
                    idFutureMap.put(unitConfig.getId(), future);
                } else {
                    // unit is not a device so just start a task
                    idFutureMap.put(unitConfig.getId(), createQueryTask(unitConfig, deviceState, syncObject));
                }
            } catch (CouldNotPerformException ex) {
                setError(deviceState, ex, ErrorCode.UNKNOWN_ERROR);
            }
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
     * Create a query task that will fill the device state according the the state of a unit.
     * The unit is given by its config and all actions on the device state are synchronized using the syncObject.
     *
     * @param unitConfig  the unit config for which a remote is created and its state synchronized into the device state
     * @param deviceState the device state into which the unit state is filled
     * @param syncObject  object used to synchronize every access to the device state
     *
     * @return a future for the created task
     */
    private static Future createQueryTask(final UnitConfig unitConfig, final JsonObject deviceState, final SyncObject syncObject) {
        return GlobalCachedExecutorService.submit((Callable<Void>) () -> {
            try {
                // get unit remote by id
                final UnitRemote unitRemote = Units.getUnit(unitConfig, false);

                try {
                    // wait for data
                    unitRemote.waitForData(UNIT_DATA_TIMEOUT, TimeUnit.SECONDS);
                } catch (CouldNotPerformException ex) {
                    // this is thrown on a timeout so assume the unit is not online
                    synchronized (syncObject) {
                        deviceState.addProperty(DEBUG_CODE_KEY, ex.getMessage());
                        deviceState.addProperty("online", false);
                    }
                    return null;
                }
                // received data so unit is online
                synchronized (syncObject) {
                    deviceState.addProperty("online", true);

                    // map state of unit remote into device state object
                    UnitDataMapper.getByType(unitRemote.getUnitType()).map(unitRemote, deviceState);
                }
            } catch (NotAvailableException ex) {
                // thrown if the unit remote is not available so propagate deviceNotFound error
                synchronized (syncObject) {
                    setError(deviceState, ex, ErrorCode.DEVICE_NOT_FOUND);
                }
            } catch (InterruptedException ex) {
                // interrupted so propagate a timeout error
                synchronized (syncObject) {
                    setError(deviceState, ex, ErrorCode.TIMEOUT);
                }
            }

            return null;
        });
    }

    /**
     * Handle an execute intent from the Google Assistant.
     * This method will fill the payload object according to this intents specification
     * and the received input.
     *
     * @param payload the payload of the response send to Google.
     * @param input   the input object as send by Google
     */
    private static void handleExecute(final JsonObject payload, final JsonObject input, final String authenticationToken, final String authorizationToken) {
        // create a map which has the unit id as key and a list of commands to execute as value
        final Map<String, List<JsonObject>> idCommandMap = new HashMap<>();
        // iterate over all commands
        for (JsonElement commandElem : input.getAsJsonObject(PAYLOAD_KEY).getAsJsonArray(COMMANDS_KEY)) {
            // treat command as json object
            final JsonObject command = commandElem.getAsJsonObject();

            // convert all execution objects in the command into a list
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

        // if there is more than one command ignore infrastructure units
        final boolean filterInfrastructureUnits = idCommandMap.size() > 1;

        final Map<String, Future<Void>> idFutureMap = new HashMap<>();
        // iterate over all entries, start according tasks to execute the given commands and save futures of these tasks
        for (final Entry<String, List<JsonObject>> idCommand : idCommandMap.entrySet()) {
            try {
                final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(idCommand.getKey());
                final Future<Void> future;

                if (unitConfig.getUnitType() == UnitType.DEVICE) {
                    // device was used to group some dal units together so create a task which will handle all internal
                    // dal unit tasks
                    future = GlobalCachedExecutorService.submit((Callable<Void>) () -> {
                        final Set<Future<Void>> internalFutureSet = new HashSet<>();
                        try {
                            // create tasks for all grouped dal units
                            for (final UnitConfig hostedUnitConfig : getUnitConfigsHandledByDevice(unitConfig)) {
                                // filter infrastructure units
                                if (filterInfrastructureUnits && Units.getUnit(unitConfig, false).isInfrastructure()) {
                                    // add a completed future, this will make google respond with a success message
                                    internalFutureSet.add(FutureProcessor.completedFuture(null));
                                } else {
                                    internalFutureSet.add(createExecutionTask(Units.getUnit(hostedUnitConfig, false), idCommand.getValue(), authenticationToken, authorizationToken));
                                }
                            }

                            // wait for all tasks and gather exceptions
                            ExceptionStack exceptionStack = null;
                            for (final Future<Void> internalFuture : internalFutureSet) {
                                try {
                                    // timeout is not needed because the created tasks are implemented in a way that
                                    // they will no block forever
                                    internalFuture.get();
                                } catch (ExecutionException ex) {
                                    // gather throws exception on stack
                                    exceptionStack = MultiException.push(internalFuture, ex, exceptionStack);
                                    /* Note: It could be differentiated by exception cause as done below to allow the
                                     * code below to handle the failure of this future correctly. However, it is not
                                     * clear how this should be done. What if one dal unit fails because it is offline
                                     * and another could not execute the given action. Therefore it is not differentiated
                                     * and this task fails if one internal task fails.
                                     */
                                }
                            }
                            // throw exception if at least one internal task has failed
                            MultiException.checkAndThrow(() -> "Could not execute one command for internal units of device[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "]", exceptionStack);
                        } catch (InterruptedException ex) {
                            // interrupted, so cancel all internal tasks and finish normally
                            for (final Future<Void> internalFuture : internalFutureSet) {
                                if (!internalFuture.isDone()) {
                                    internalFuture.cancel(true);
                                }
                            }
                        }
                        return null;
                    });
                } else {
                    // filter infrastructure units
                    if (filterInfrastructureUnits && Units.getUnit(unitConfig, false).isInfrastructure()) {
                        // add a completed future, this will make google respond with a success message
                        future = FutureProcessor.completedFuture(null);
                    } else {
                        // dal unit so create a normal execution task
                        future = createExecutionTask(Units.getUnit(unitConfig, false), idCommand.getValue(), authenticationToken, authorizationToken);
                    }
                }

                // add future to a map
                idFutureMap.put(idCommand.getKey(), future);
            } catch (CouldNotPerformException ex) {
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
            // iterate and wait for every task to validate its execution
            for (Entry<String, Future<Void>> entry : idFutureMap.entrySet()) {
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
                        ExceptionPrinter.printHistory("Could not perform execute commands for unit[" + entry.getKey() + "]", ex, LOGGER);
                    } else if (ex.getCause() instanceof java.util.concurrent.TimeoutException) {
                        // cause was a timeout on waiting for operation service return so still pending
                        pending.add(entry.getKey());
                        ExceptionPrinter.printHistory("Could not perform execute commands for unit[" + entry.getKey() + "]", ex, LOGGER);
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
     * Create a task and returns its future that will try to execute some actions for a unit remote.
     * This task will not block forever because on all internal tasks is waited with a timeout.
     *
     * @param unitRemote    the unit remote on which the actions will be executed
     * @param executionList a list of json objects defining the actions to be executed
     *
     * @return a future of the task created that will execute all actions for the given unit remote
     */
    private static Future<Void> createExecutionTask(final UnitRemote<?> unitRemote, final List<JsonObject> executionList, final String authenticationToken, final String authorizationToken) {
        return GlobalCachedExecutorService.submit((Callable<Void>) () -> {
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
            for (final JsonObject execution : executionList) {
                // extract command name and param object
                final String commandName = execution.get(COMMAND_KEY).getAsString();
                final JsonObject params = execution.getAsJsonObject(PARAMS_KEY);

                // find command type by name
                final Command commandType = Command.getByRepresentation(commandName);
                // find trait by command type and params
                final Trait trait = Trait.getByCommand(commandType, params);
                // resolve unit type mapping for remote
                final UnitTypeMapping unitTypeMapping;
                if (unitRemote.getUnitType() == UnitType.UNIT_GROUP) {
                    unitTypeMapping = UnitTypeMapping.getByUnitType(unitRemote.getConfig().getUnitGroupConfig().getUnitType());
                } else {
                    unitTypeMapping = UnitTypeMapping.getByUnitType(unitRemote.getUnitType());
                }
                // get service type for trait
                final ServiceType serviceType = unitTypeMapping.getServiceType(trait);
                // service type is null if the given command is not supported by this unit
                if (serviceType == null) {
                    continue;
                }
                // resolve mapping for the combination of service type and trait
                final ServiceStateTraitMapper serviceStateTraitMapper = ServiceStateTraitMapperFactory.getInstance().getServiceStateMapper(serviceType, trait);
                // parse trait param into service state
                final Message serviceState = serviceStateTraitMapper.map(params, commandType);

                final ActionDescription.Builder actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(serviceState, serviceType, unitRemote);
                actionDescription.setPriority(Priority.HIGH);
                actionDescription.setAutoContinueWithLowPriority(true);
                final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescription.build(), AuthToken.newBuilder().setAuthenticationToken(authenticationToken).setAuthorizationToken(authorizationToken).build());

                final AuthenticatedValueFuture<ActionDescription> ActionDescriptionAuthenticatedValueFuture = new AuthenticatedValueFuture<>(unitRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());

                // wait for result
                try {
                    ActionDescriptionAuthenticatedValueFuture.get(UNIT_TASK_TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    // cancel internal task and finish normally
                    ActionDescriptionAuthenticatedValueFuture.cancel(true);
                } catch (ExecutionException ex) {
                    // throw exception to inform task waiting for this one
                    throw new CouldNotPerformException("Invoking service[" + serviceType.name() + "] " +
                            "operation method failed for unit[" + unitRemote + "]", ex);
                }
            }

            return null;
        });
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
    private static void addExecuteResults(final JsonArray commands, final Set<String> idSet, final String resultType) {
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
        if (!ExceptionProcessor.isCausedBySystemShutdown(exception) && !ExceptionProcessor.isCausedByInterruption(exception)) {
            ExceptionPrinter.printHistory(exception, LOGGER);
        }
    }

    private static void setError(final JsonObject jsonObject, final String debugString, final ErrorCode errorCode) {
        jsonObject.addProperty(ERROR_CODE_KEY, errorCode.toString());
        jsonObject.addProperty(DEBUG_CODE_KEY, debugString);
    }
}
