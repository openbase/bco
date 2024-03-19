package org.openbase.bco.device.openhab.communication

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import jakarta.ws.rs.core.MediaType
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InitializationException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.iface.Shutdownable
import org.openhab.core.config.discovery.dto.DiscoveryResultDTO
import org.openhab.core.io.rest.core.item.EnrichedItemDTO
import org.openhab.core.io.rest.core.thing.EnrichedThingDTO
import org.openhab.core.items.dto.ItemDTO
import org.openhab.core.thing.dto.ThingDTO
import org.openhab.core.thing.link.dto.ItemChannelLinkDTO
import org.openhab.core.types.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OpenHABRestCommunicator private constructor() : OpenHABRestConnection() {
    // ==========================================================================================================================================
    // THINGS
    // ==========================================================================================================================================
    @Throws(CouldNotPerformException::class)
    fun registerThing(thingDTO: ThingDTO?): EnrichedThingDTO {
        return jsonToClass(JsonParser.parseString(postJson(THINGS_TARGET, thingDTO)), EnrichedThingDTO::class.java)
    }

    @Throws(CouldNotPerformException::class)
    fun updateThing(enrichedThingDTO: EnrichedThingDTO): EnrichedThingDTO {
        return jsonToClass(
            JsonParser.parseString(
                putJson(
                    THINGS_TARGET + SEPARATOR + enrichedThingDTO.UID,
                    enrichedThingDTO
                )
            ), EnrichedThingDTO::class.java
        )
    }

    @Throws(CouldNotPerformException::class)
    fun deleteThing(enrichedThingDTO: EnrichedThingDTO): EnrichedThingDTO {
        return deleteThing(enrichedThingDTO.UID)
    }

    @Throws(CouldNotPerformException::class)
    fun deleteThing(thingUID: String): EnrichedThingDTO {
        return jsonToClass(
            JsonParser.parseString(delete(THINGS_TARGET + SEPARATOR + thingUID)),
            EnrichedThingDTO::class.java
        )
    }

    @Throws(NotAvailableException::class)
    fun getThing(thingUID: String): EnrichedThingDTO {
        try {
            return jsonToClass(
                JsonParser.parseString(get(THINGS_TARGET + SEPARATOR + thingUID)),
                EnrichedThingDTO::class.java
            )
        } catch (ex: CouldNotPerformException) {
            throw NotAvailableException("Thing[$thingUID]")
        }
    }

    @get:Throws(CouldNotPerformException::class)
    val things: List<EnrichedThingDTO>
        get() = jsonElementToTypedList(JsonParser.parseString(get(THINGS_TARGET)), EnrichedThingDTO::class.java)

    // ==========================================================================================================================================
    // ITEMS
    // ==========================================================================================================================================
    @Throws(CouldNotPerformException::class)
    fun registerItem(itemDTO: ItemDTO): ItemDTO {
        val itemDTOList: MutableList<ItemDTO> = ArrayList()
        itemDTOList.add(itemDTO)
        return registerItems(itemDTOList)[0]
    }

    @Throws(CouldNotPerformException::class)
    fun registerItems(itemDTOList: List<ItemDTO>?): List<ItemDTO> {
        return jsonElementToTypedList(JsonParser.parseString(putJson(ITEMS_TARGET, itemDTOList)), ItemDTO::class.java)
    }

    @Throws(CouldNotPerformException::class)
    fun updateItem(itemDTO: ItemDTO): ItemDTO {
        return jsonToClass(
            JsonParser.parseString(putJson(ITEMS_TARGET + SEPARATOR + itemDTO.name, itemDTO)),
            ItemDTO::class.java
        )
    }

    @Throws(CouldNotPerformException::class)
    fun deleteItem(itemDTO: ItemDTO): ItemDTO = deleteItem(itemDTO.name)

    @Throws(CouldNotPerformException::class)
    fun deleteItem(itemName: String): ItemDTO {
        LOGGER.warn("Delete item {}", itemName)
        return jsonToClass(JsonParser.parseString(delete(ITEMS_TARGET + SEPARATOR + itemName)), ItemDTO::class.java)
    }

    @get:Throws(CouldNotPerformException::class)
    val items: List<EnrichedItemDTO>
        get() = jsonElementToTypedList(JsonParser.parseString(get(ITEMS_TARGET)), EnrichedItemDTO::class.java)

    @Throws(NotAvailableException::class)
    fun getItem(itemName: String): EnrichedItemDTO =
        try {
            jsonToClass(
                JsonParser.parseString(get(ITEMS_TARGET + SEPARATOR + itemName)),
                EnrichedItemDTO::class.java
            )
        } catch (ex: CouldNotPerformException) {
            throw NotAvailableException("Item with name[$itemName]")
        }

    fun hasItem(itemName: String): Boolean {
        try {
            getItem(itemName)
            return true
        } catch (ex: NotAvailableException) {
            return false
        }
    }

    @Throws(CouldNotPerformException::class)
    fun postCommand(itemName: String, command: Command) {
        postCommand(itemName, command.toString())
    }

    @Throws(CouldNotPerformException::class)
    fun postCommand(itemName: String, command: String?) {
        post(ITEMS_TARGET + SEPARATOR + itemName, command!!, MediaType.TEXT_PLAIN_TYPE)
    }

    // ==========================================================================================================================================
    // ITEM_CHANNEL_LINK
    // ==========================================================================================================================================
    @Throws(CouldNotPerformException::class)
    fun registerItemChannelLink(itemName: String?, channelUID: String?) {
        registerItemChannelLink(ItemChannelLinkDTO(itemName, channelUID, HashMap()))
    }

    @Throws(CouldNotPerformException::class)
    fun registerItemChannelLink(itemChannelLinkDTO: ItemChannelLinkDTO) {
        putJson(
            LINKS_TARGET + SEPARATOR + itemChannelLinkDTO.itemName + SEPARATOR + itemChannelLinkDTO.channelUID,
            itemChannelLinkDTO
        )
    }

    @Throws(CouldNotPerformException::class)
    fun deleteItemChannelLink(itemChannelLinkDTO: ItemChannelLinkDTO) {
        deleteItemChannelLink(itemChannelLinkDTO.itemName, itemChannelLinkDTO.channelUID)
    }

    @Throws(CouldNotPerformException::class)
    fun deleteItemChannelLink(itemName: String, channelUID: String) {
        delete(LINKS_TARGET + SEPARATOR + itemName + SEPARATOR + channelUID)
    }

    @get:Throws(CouldNotPerformException::class)
    val itemChannelLinks: List<ItemChannelLinkDTO>
        get() = jsonElementToTypedList(JsonParser.parseString(get(LINKS_TARGET)), ItemChannelLinkDTO::class.java)

    // ==========================================================================================================================================
    // DISCOVERY
    // ==========================================================================================================================================
    /**
     * @param bindingId
     *
     * @return the discovery timeout in seconds
     *
     * @throws CouldNotPerformException
     */
    @Throws(CouldNotPerformException::class)
    fun startDiscovery(bindingId: String): Int {
        val response = post(
            DISCOVERY_TARGET + SEPARATOR + BINDINGS_TARGET + SEPARATOR + bindingId + SCAN_TARGET,
            "",
            MediaType.APPLICATION_JSON_TYPE
        )
        val discoveryTimeout = response.toInt()

        if (discoveryTimeout <= 0) {
            throw CouldNotPerformException("Invalid discovery timeout. Maybe binding $bindingId is not available")
        }

        return discoveryTimeout
    }

    @Throws(CouldNotPerformException::class)
    fun approve(thingUID: String, label: String?) {
        post(INBOX_TARGET + SEPARATOR + thingUID + SEPARATOR + APPROVE_TARGET, label!!, MediaType.TEXT_PLAIN_TYPE)
    }

    @get:Throws(CouldNotPerformException::class)
    val discoveryResults: List<DiscoveryResultDTO>
        get() = jsonElementToTypedList(JsonParser.parseString(get(INBOX_TARGET)), DiscoveryResultDTO::class.java)

    // ==========================================================================================================================================
    // Extensions
    // ==========================================================================================================================================
    @Throws(CouldNotPerformException::class)
    fun installBinding(bindingId: String) {
        LOGGER.debug("Install Binding[$bindingId]")
        post(
            ADDONS_TARGET + SEPARATOR + ADDONS_BINDING_PREFIX + bindingId + SEPARATOR + INSTALL_TARGET,
            "",
            MediaType.APPLICATION_JSON_TYPE
        )
    }

    fun isBindingInstalled(bindingId: String): Boolean {
        try {
            get(BINDINGS_TARGET + SEPARATOR + bindingId + SEPARATOR + CONFIG_TARGET)
            LOGGER.debug("Binding[$bindingId] currently not installed!")
            return true
        } catch (ex: CouldNotPerformException) {
            LOGGER.debug("Binding[$bindingId] is already installed.")
            return false
        }
    }

    @Throws(CouldNotPerformException::class)
    fun uninstallBindings(bindingId: String) {
        post(ADDONS_TARGET + SEPARATOR + bindingId + SEPARATOR + UNINSTALL_TARGET, "", MediaType.APPLICATION_JSON_TYPE)
    }

    // ==========================================================================================================================================
    // UTIL
    // ==========================================================================================================================================
    @Throws(CouldNotPerformException::class)
    private fun <T> jsonElementToTypedList(jsonElement: JsonElement, clazz: Class<T>): List<T> {
        if (jsonElement.isJsonArray) {
            return jsonArrayToTypedList(jsonElement.asJsonArray, clazz)
        } else {
            throw CouldNotPerformException("JsonElement is not a JsonArray and thus cannot be converted to a list")
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun <T> jsonArrayToTypedList(jsonArray: JsonArray, clazz: Class<T>): List<T> {
        val result: MutableList<T> = ArrayList()

        for (jsonElement in jsonArray) {
            result.add(jsonToClass(jsonElement, clazz))
        }

        return result
    }

    @Throws(CouldNotPerformException::class)
    private fun <T> jsonToClass(jsonElement: JsonElement, clazz: Class<T>): T {
        try {
            return gson.fromJson(jsonElement, clazz)
        } catch (ex: JsonSyntaxException) {
            throw CouldNotPerformException(
                "Could not parse jsonElement into object of class[" + clazz.simpleName + "]",
                ex
            )
        }
    }

    @Throws(CouldNotPerformException::class)
    override fun testConnection() {
        get(INBOX_TARGET, true)
    }

    companion object {
        const val ITEMS_TARGET: String = "items"
        const val LINKS_TARGET: String = "links"
        const val THINGS_TARGET: String = "things"
        const val INBOX_TARGET: String = "inbox"
        const val DISCOVERY_TARGET: String = "discovery"
        const val ADDONS_TARGET: String = "addons"
        const val ADDONS_BINDING_PREFIX: String = "binding-"
        const val INSTALL_TARGET: String = "install"
        const val UNINSTALL_TARGET: String = "uninstall"
        const val BINDINGS_TARGET: String = "bindings"
        const val CONFIG_TARGET: String = "config"
        const val SCAN_TARGET: String = "scan"

        private val LOGGER: Logger = LoggerFactory.getLogger(OpenHABRestCommunicator::class.java)

        @JvmStatic
        @get:Synchronized
        var instance: OpenHABRestCommunicator? = null
            get() {
                if (field == null) {
                    try {
                        field = OpenHABRestCommunicator()
                        Shutdownable.registerShutdownHook(field)
                    } catch (ex: InitializationException) {
                        ExceptionPrinter.printHistory("Could not create OpenHABRestCommunicator", ex, LOGGER)
                    } catch (ex: CouldNotPerformException) {
                        // only thrown if instance would be null
                    }
                }

                return field
            }
            private set
    }
}
