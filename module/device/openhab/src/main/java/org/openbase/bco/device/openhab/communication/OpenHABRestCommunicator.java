package org.openbase.bco.device.openhab.communication;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Shutdownable;
import org.openhab.core.config.discovery.dto.DiscoveryResultDTO;
import org.openhab.core.io.rest.core.item.EnrichedItemDTO;
import org.openhab.core.io.rest.core.thing.EnrichedThingDTO;
import org.openhab.core.items.dto.ItemDTO;
import org.openhab.core.thing.dto.ThingDTO;
import org.openhab.core.thing.link.dto.ItemChannelLinkDTO;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpenHABRestCommunicator extends OpenHABRestConnection {


    public static final String ITEMS_TARGET = "items";
    public static final String LINKS_TARGET = "links";
    public static final String THINGS_TARGET = "things";
    public static final String INBOX_TARGET = "inbox";
    public static final String DISCOVERY_TARGET = "discovery";
    public static final String ADDONS_TARGET = "addons";
    public static final String ADDONS_BINDING_PREFIX = "binding-";
    public static final String INSTALL_TARGET = "install";
    public static final String UNINSTALL_TARGET = "uninstall";
    public static final String BINDINGS_TARGET = "bindings";
    public static final String CONFIG_TARGET = "config";
    public static final String SCAN_TARGET = "scan";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABRestCommunicator.class);

    private static OpenHABRestCommunicator instance = null;

    public synchronized static OpenHABRestCommunicator getInstance() {
        if (instance == null) {
            try {
                instance = new OpenHABRestCommunicator();
                Shutdownable.registerShutdownHook(instance);
            } catch (InitializationException ex) {
                ExceptionPrinter.printHistory("Could not create OpenHABRestCommunicator", ex, LOGGER);
            } catch (CouldNotPerformException ex) {
                // only thrown if instance would be null
            }
        }

        return instance;
    }

    private OpenHABRestCommunicator() throws InstantiationException {
        super();
    }

    // ==========================================================================================================================================
    // THINGS
    // ==========================================================================================================================================

    public EnrichedThingDTO registerThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(postJson(THINGS_TARGET, thingDTO)), EnrichedThingDTO.class);
    }

    public EnrichedThingDTO updateThing(final EnrichedThingDTO enrichedThingDTO) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(putJson(THINGS_TARGET + SEPARATOR + enrichedThingDTO.UID, enrichedThingDTO)), EnrichedThingDTO.class);
    }

    public EnrichedThingDTO deleteThing(final EnrichedThingDTO enrichedThingDTO) throws CouldNotPerformException {
        return deleteThing(enrichedThingDTO.UID);
    }

    public EnrichedThingDTO deleteThing(final String thingUID) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(delete(THINGS_TARGET + SEPARATOR + thingUID)), EnrichedThingDTO.class);
    }

    public EnrichedThingDTO getThing(final String thingUID) throws NotAvailableException {
        try {
            return jsonToClass(jsonParser.parse(get(THINGS_TARGET + SEPARATOR + thingUID)), EnrichedThingDTO.class);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Thing[" + thingUID + "]");
        }
    }

    public List<EnrichedThingDTO> getThings() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(THINGS_TARGET)), EnrichedThingDTO.class);
    }

    // ==========================================================================================================================================
    // ITEMS
    // ==========================================================================================================================================

    public ItemDTO registerItem(final ItemDTO itemDTO) throws CouldNotPerformException {
        final List<ItemDTO> itemDTOList = new ArrayList<>();
        itemDTOList.add(itemDTO);
        return registerItems(itemDTOList).get(0);
    }

    public List<ItemDTO> registerItems(final List<ItemDTO> itemDTOList) throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(putJson(ITEMS_TARGET, itemDTOList)), ItemDTO.class);
    }

    public ItemDTO updateItem(final ItemDTO itemDTO) throws CouldNotPerformException {
        return jsonToClass(jsonParser.parse(putJson(ITEMS_TARGET + SEPARATOR + itemDTO.name, itemDTO)), ItemDTO.class);
    }

    public ItemDTO deleteItem(final ItemDTO itemDTO) throws CouldNotPerformException {
        return deleteItem(itemDTO.name);
    }

    public ItemDTO deleteItem(final String itemName) throws CouldNotPerformException {
        LOGGER.warn("Delete item {}", itemName);
        return jsonToClass(jsonParser.parse(delete(ITEMS_TARGET + SEPARATOR + itemName)), ItemDTO.class);
    }

    public List<EnrichedItemDTO> getItems() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(ITEMS_TARGET)), EnrichedItemDTO.class);
    }

    public EnrichedItemDTO getItem(final String itemName) throws NotAvailableException {
        try {
            return jsonToClass(jsonParser.parse(get(ITEMS_TARGET + SEPARATOR + itemName)), EnrichedItemDTO.class);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Item with name[" + itemName + "]");
        }
    }

    public boolean hasItem(final String itemName) {
        try {
            getItem(itemName);
            return true;
        } catch (NotAvailableException ex) {
            return false;
        }
    }

    public void postCommand(final String itemName, final Command command) throws CouldNotPerformException {
        postCommand(itemName, command.toString());
    }

    public void postCommand(final String itemName, final String command) throws CouldNotPerformException {
        post(ITEMS_TARGET + SEPARATOR + itemName, command, MediaType.TEXT_PLAIN_TYPE);
    }

    // ==========================================================================================================================================
    // ITEM_CHANNEL_LINK
    // ==========================================================================================================================================

    public void registerItemChannelLink(final String itemName, final String channelUID) throws CouldNotPerformException {
        registerItemChannelLink(new ItemChannelLinkDTO(itemName, channelUID, new HashMap<>()));
    }

    public void registerItemChannelLink(final ItemChannelLinkDTO itemChannelLinkDTO) throws CouldNotPerformException {
        putJson(LINKS_TARGET + SEPARATOR + itemChannelLinkDTO.itemName + SEPARATOR + itemChannelLinkDTO.channelUID, itemChannelLinkDTO);
    }

    public void deleteItemChannelLink(final ItemChannelLinkDTO itemChannelLinkDTO) throws CouldNotPerformException {
        deleteItemChannelLink(itemChannelLinkDTO.itemName, itemChannelLinkDTO.channelUID);
    }

    public void deleteItemChannelLink(final String itemName, final String channelUID) throws CouldNotPerformException {
        delete(LINKS_TARGET + SEPARATOR + itemName + SEPARATOR + channelUID);
    }

    public List<ItemChannelLinkDTO> getItemChannelLinks() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(LINKS_TARGET)), ItemChannelLinkDTO.class);
    }

    // ==========================================================================================================================================
    // DISCOVERY
    // ==========================================================================================================================================

    /**
     * @param bindingId
     * @return the discovery timeout in seconds
     * @throws CouldNotPerformException
     */
    public Integer startDiscovery(final String bindingId) throws CouldNotPerformException {
        final String response = post(DISCOVERY_TARGET + SEPARATOR + BINDINGS_TARGET + SEPARATOR + bindingId + SCAN_TARGET, "", MediaType.APPLICATION_JSON_TYPE);
        int discoveryTimeout = Integer.parseInt(response);

        if (discoveryTimeout <= 0) {
            throw new CouldNotPerformException("Invalid discovery timeout. Maybe binding " + bindingId + " is not available");
        }

        return discoveryTimeout;
    }

    public void approve(final String thingUID, final String label) throws CouldNotPerformException {
        post(INBOX_TARGET + SEPARATOR + thingUID + SEPARATOR + APPROVE_TARGET, label, MediaType.TEXT_PLAIN_TYPE);
    }

    public List<DiscoveryResultDTO> getDiscoveryResults() throws CouldNotPerformException {
        return jsonElementToTypedList(jsonParser.parse(get(INBOX_TARGET)), DiscoveryResultDTO.class);
    }

    // ==========================================================================================================================================
    // Extensions
    // ==========================================================================================================================================

    public void installBinding(final String bindingId) throws CouldNotPerformException {
        LOGGER.debug("Install Binding[" + bindingId + "]");
        post(ADDONS_TARGET + SEPARATOR + ADDONS_BINDING_PREFIX + bindingId + SEPARATOR + INSTALL_TARGET, "", MediaType.APPLICATION_JSON_TYPE);
    }

    public boolean isBindingInstalled(final String bindingId) {
        try {
            get(BINDINGS_TARGET + SEPARATOR + bindingId + SEPARATOR + CONFIG_TARGET);
            LOGGER.debug("Binding[" + bindingId + "] currently not installed!");
            return true;
        } catch (CouldNotPerformException ex) {
            LOGGER.debug("Binding[" + bindingId + "] is already installed.");
            return false;
        }
    }

    public void uninstallBindings(final String bindingId) throws CouldNotPerformException {
        post(ADDONS_TARGET + SEPARATOR + bindingId + SEPARATOR + UNINSTALL_TARGET, "", MediaType.APPLICATION_JSON_TYPE);
    }

    // ==========================================================================================================================================
    // UTIL
    // ==========================================================================================================================================

    private <T> List<T> jsonElementToTypedList(final JsonElement jsonElement, final Class<T> clazz) throws CouldNotPerformException {
        if (jsonElement.isJsonArray()) {
            return jsonArrayToTypedList(jsonElement.getAsJsonArray(), clazz);
        } else {
            throw new CouldNotPerformException("JsonElement is not a JsonArray and thus cannot be converted to a list");
        }
    }

    private <T> List<T> jsonArrayToTypedList(final JsonArray jsonArray, final Class<T> clazz) throws CouldNotPerformException {
        final List<T> result = new ArrayList<T>();

        for (final JsonElement jsonElement : jsonArray) {
            result.add(jsonToClass(jsonElement, clazz));
        }

        return result;
    }

    private <T> T jsonToClass(final JsonElement jsonElement, final Class<T> clazz) throws CouldNotPerformException {
        try {
            return gson.fromJson(jsonElement, clazz);
        } catch (JsonSyntaxException ex) {
            throw new CouldNotPerformException("Could not parse jsonElement into object of class[" + clazz.getSimpleName() + "]", ex);
        }
    }

    @Override
    protected void testConnection() throws CouldNotPerformException {
        get(INBOX_TARGET, true);
    }
}
