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
import org.openbase.bco.app.cloud.connector.google.ServiceTypeTraitMapping;
import org.openbase.bco.app.cloud.connector.google.Trait;
import org.openbase.bco.app.cloud.connector.google.UnitTypeDeviceTypeMapping;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    public static final String INPUTS_KEY = "inputs";
    public static final String INTENT_KEY = "intent";
    public static final String COMMANDS_KEY = "commands";
    public static final String EXECUTION_KEY = "execution";
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String PAYLOAD_KEY = "payload";
    public static final String AGENT_USER_ID_KEY = "agentUserId";
    public static final String ID_KEY = "id";
    public static final String TYPE_KEY = "type";
    public static final String TRAITS_KEY = "traits";
    public static final String NAME_KEY = "name";
    public static final String DEVICES_KEY = "devices";

    private static final Logger LOGGER = LoggerFactory.getLogger(FulfillmentHandler.class);

    /**
     * Handle any smart home request from Google. This is done by parsing the intent
     * and invoking according methods for each.
     *
     * @param request the request from Google parsed into a JsonObject.
     * @return a JsonObject which is the answer for the request
     * @throws CouldNotPerformException if the request could not be performed
     */
    public JsonObject handleRequest(final JsonObject request) throws CouldNotPerformException {
        // parse request id, input and intent
        final String requestId = request.get(REQUEST_ID_KEY).getAsString();
        final JsonObject input = request.getAsJsonArray(INPUTS_KEY).get(0).getAsJsonObject();
        final String intent = input.get(INTENT_KEY).getAsString();

        // build basic response type, by adding the request id and the payload
        final JsonObject response = new JsonObject();
        final JsonObject payload = new JsonObject();
        response.addProperty(REQUEST_ID_KEY, requestId);
        response.add(PAYLOAD_KEY, payload);

        // decide what to do based on the intent
        switch (intent) {
            case SYNC_INTENT:
                // method will handle a sync intent and fill the payload of the response accordingly
                handleSync(payload);
                break;
            case QUERY_INTENT:
                // method will handle a query intent and fill the payload of the response accordingly
                handleQuery(payload, input);
                break;
            case EXECUTE_INTENT:
                // method will handle an execute intent and fill the payload of the response accordingly
                handleExecute(payload, input);
                break;
            default:
                throw new CouldNotPerformException("Unknown intent[" + intent + "]");
        }

        return response;
    }

    private void handleSync(final JsonObject payload) throws CouldNotPerformException {
        // TODO: debug string and error optional for payload

        // TODO: this has to be an id for a bco instance
        payload.addProperty(AGENT_USER_ID_KEY, UUID.randomUUID().toString());

        final JsonArray devices = new JsonArray();
        try {
            for (final UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs()) {
                final JsonObject device = new JsonObject();

                try {
                    final UnitTypeDeviceTypeMapping unitTypeDeviceTypeMapping = UnitTypeDeviceTypeMapping.getByUnitType(unitConfig.getType());
                    device.addProperty(TYPE_KEY, unitTypeDeviceTypeMapping.getDeviceType().getRepresentation());
                } catch (NotAvailableException ex) {
                    LOGGER.warn("Skip unit[" + unitConfig.getAlias(0) + "] because no mapping for unitType[" + unitConfig.getType().name() + "] available");
                    continue;
                }

                device.addProperty(ID_KEY, unitConfig.getId());
                //TODO: maybe we want this
                device.addProperty("willReportState", false);
                device.addProperty("roomHint", Registries.getUnitRegistry().getUnitConfigById(unitConfig.getPlacementConfig().getLocationId()).getLabel());

                final JsonObject attributes = new JsonObject();
                final JsonArray traits = new JsonArray();
                for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getPattern() != ServicePattern.OPERATION) {
                        //TODO: this is only for testing, instead just filter different patterns
                        continue;
                    }
                    final ServiceType serviceType = serviceConfig.getServiceDescription().getType();

                    //TODO: how to add attributes for a trait?
                    try {
                        ServiceTypeTraitMapping serviceTypeTraitMapping = ServiceTypeTraitMapping.getByServiceType(serviceType);
                        for (final Trait trait : serviceTypeTraitMapping.getTraitSet()) {
                            traits.add(trait.getRepresentation());
                            trait.getTraitMapper().addAttributes(unitConfig, attributes);
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
                name.addProperty("name", unitConfig.getLabel());
                final JsonArray defaultNames = new JsonArray();
                for (final String alias : unitConfig.getAliasList()) {
                    defaultNames.add(alias.replace("-", " "));
                }
                name.add("defaultNames", defaultNames);
                //TODO: nicknames are missing
                device.add(NAME_KEY, name);

                final JsonObject deviceInfo = new JsonObject();
//                deviceInfo.addProperty("manufacturer", Registries.getClassRegistry(true).getDeviceClassById(Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId()).getDeviceConfig().getDeviceClassId()).getCompany());
                //TODO: missing: model, hwVersion, swVersion
                device.add("deviceInfo", deviceInfo);

                //TODO: custom data missing
                devices.add(device);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Interrupted while running");
        }
        payload.add(DEVICES_KEY, devices);
    }

    private void handleQuery(final JsonObject payload, final JsonObject input) throws CouldNotPerformException {
        final List<String> deviceIdList = new ArrayList<>();
        for (JsonElement elem : input.getAsJsonObject(PAYLOAD_KEY).getAsJsonArray(DEVICES_KEY)) {
            deviceIdList.add(elem.getAsJsonObject().get(ID_KEY).getAsString());
        }

        JsonObject devices = new JsonObject();
        payload.add(DEVICES_KEY, devices);

        for (final String id : deviceIdList) {
            try {
                final JsonObject deviceState = new JsonObject();
                //TODO waiting has to be done with a timeout, else add an error
                UnitRemote unitRemote = Units.getUnit(id, true);
                try {
                    unitRemote.waitForData();
                } catch (NotAvailableException ex) {
                    // thrown if data is not available
                    deviceState.addProperty("online", false);
                    continue;
                }

                deviceState.addProperty("online", true);
                final Set<ServiceType> serviceTypeSet = new HashSet<>();
//                for (ServiceDescription serviceDescription : Registries.getTemplateRegistry(true).getUnitTemplateByType(unitRemote.getUnitType()).getServiceDescriptionList()) {
//                    if (serviceTypeSet.contains(serviceDescription.getType())) {
//                        continue;
//                    }
//                    serviceTypeSet.add(serviceDescription.getType());
//
//                    ServiceTypeTraitMapping byServiceType = ServiceTypeTraitMapping.getByServiceType(serviceDescription.getType());
//                    for (Trait trait : byServiceType.getTraitSet()) {
//                        try {
//                            trait.getTraitMapper().map((GeneratedMessage) Services.invokeProviderServiceMethod(serviceDescription.getType(), unitRemote), deviceState);
//                        } catch (CouldNotPerformException ex) {
//                            LOGGER.warn("Skip service[" + serviceDescription.getType().name() + "] for unit[" + unitRemote + "]");
//                        }
//                    }
//                }

                devices.add(id, deviceState);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CouldNotPerformException("Interrupted while running", ex);
            }
        }
    }

    private void handleExecute(final JsonObject payLoad, final JsonObject input) throws CouldNotPerformException {
        final Map<String, List<JsonObject>> idCommandMap = new HashMap<>();
        for (JsonElement elem : input.getAsJsonObject(PAYLOAD_KEY).getAsJsonArray(COMMANDS_KEY)) {
            final JsonObject command = elem.getAsJsonObject();

            List<JsonObject> executionList = new ArrayList<>();
            for (JsonElement executeElem : command.getAsJsonArray(EXECUTION_KEY)) {
                executionList.add(executeElem.getAsJsonObject());
            }

            for (JsonElement deviceElem : command.getAsJsonArray(DEVICES_KEY)) {
                final String id = deviceElem.getAsJsonObject().get(ID_KEY).getAsString();
                if (!idCommandMap.containsKey(id)) {
                    idCommandMap.put(id, new ArrayList<>(executionList));
                } else {
                    idCommandMap.get(id).addAll(executionList);
                }
            }
        }

        final JsonArray commands = new JsonArray();
        payLoad.add("commands", commands);
        final JsonObject command = new JsonObject();
        commands.add(command);
        final JsonObject state = new JsonObject();
        final JsonArray ids = new JsonArray();
        command.add("ids", ids);
        command.add("states", state);

        for (final Entry<String, List<JsonObject>> idCommand : idCommandMap.entrySet()) {
            try {
                final UnitRemote unitRemote = Units.getUnit(idCommand.getKey(), false);

                for (final JsonObject execution : idCommand.getValue()) {
                    final String commandName = execution.get("command").getAsString();

                    try {
                        //TODO: get should be called with a timeout
                        final Trait trait = Trait.getByCommand(commandName);
                        final ServiceType serviceType = trait.getTraitMapper().getServiceType();
                        final GeneratedMessage serviceState = trait.getTraitMapper().map(execution.getAsJsonObject("params"));
                        final Future serviceFuture = (Future) Services.invokeOperationServiceMethod(serviceType, unitRemote, serviceState);
                        final GeneratedMessage msg = (GeneratedMessage) serviceFuture.get();

                        LOGGER.info("ServiceState[" + msg + "] for serviceType[" + serviceType.name() + "] of unit[" + unitRemote + "]");
                        trait.getTraitMapper().map(msg, state);
                    } catch (NotAvailableException ex) {
                        LOGGER.warn("Not trait for command[" + commandName + "] available");
                    } catch (CouldNotPerformException ex) {
                        LOGGER.warn("Skip command[" + commandName + "]", ex);
                    }
                }

            } catch (InterruptedException | ExecutionException ex) {
                Thread.currentThread().interrupt();
                throw new CouldNotPerformException("Interrupted while running", ex);
            }
        }
    }
}
