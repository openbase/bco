package org.openbase.bco.device.openhab.registry.synchronizer;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.extension.protobuf.ProtobufVariableProvider;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;
import org.openbase.type.language.LabelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SynchronizationProcessor {

    public static final String OPENHAB_THING_PROPERTY_KEY_MODEL_ID = "modelId";
    public static final String OPENHAB_THING_PROPERTY_KEY_UNIQUE_ID = "uniqueId";

    public static final String OPENHAB_THING_UID_KEY = "OPENHAB_THING_UID";
    public static final String OPENHAB_THING_CLASS_KEY = "OPENHAB_THING_CLASS";
    public static final String OPENHAB_THING_CHANNEL_TYPE_UID_KEY = "OPENHAB_THING_CHANNEL_TYPE_UID";
    public static final String OPENHAB_ITEM_TYPE_KEY = "OPENHAB_ITEM_TYPE";
    public static final String OPENHAB_UNIQUE_ID_KEY = "OPENHAB_UNIQUE_ID";

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationProcessor.class);

    /**
     * Retrieve a device unit config for a thing. This is done by looking for a meta config entry in the device unit
     * config where the id of the thing is referenced.
     *
     * @param thingDTO the thing for which a device should be retrieved
     * @return a device unit config for the thing as described above
     * @throws NotAvailableException if no device could be found
     */
    public static UnitConfig getDeviceForThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        // iterate over all devices
        for (final UnitConfig deviceUnitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitTypeFiltered(UnitType.DEVICE, false)) {
            // get the most global meta config
            final MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider("UnitMetaConfig", deviceUnitConfig.getMetaConfig()));

            // bypass mapping by thing uid in case multiple things map to a single device unit
            if (thingDTO.properties.containsKey(SynchronizationProcessor.OPENHAB_THING_PROPERTY_KEY_UNIQUE_ID)) {
                try {
                    final String uniquePrefix = metaConfigPool.getValue(SynchronizationProcessor.OPENHAB_UNIQUE_ID_KEY);
                    if (thingDTO.properties.get(SynchronizationProcessor.OPENHAB_THING_PROPERTY_KEY_UNIQUE_ID).startsWith(uniquePrefix)) {
                        return deviceUnitConfig;
                    }
                } catch (NotAvailableException ex) {
                    // value not available so continue with the next check
                }
            }

            try {
                // get the value for the thing uid key
                String thingUID = metaConfigPool.getValue(OPENHAB_THING_UID_KEY);
                // if it matches with the uid of the thing return the device
                if (thingUID.equals(thingDTO.UID)) {
                    return deviceUnitConfig;
                }
            } catch (NotAvailableException ex) {
                // value for device not available so continue
            }
        }

        // throw exception because device for thing could not be found
        throw new NotAvailableException("Device for thing[" + thingDTO.UID + "]");
    }

    /**
     * Extract a prefix for a unique id by removing everything after the last minus.
     *
     * @param uniqueId the unique id
     * @return a prefix for this unique id
     */
    public static String getUniquePrefix(final String uniqueId) {
        final String[] split = uniqueId.split("-");
        final StringBuilder uniquePrefix = new StringBuilder(split[0]);
        for (int i = 1; i < split.length - 1; i++) {
            uniquePrefix.append("-");
            uniquePrefix.append(split[i]);
        }
        return uniquePrefix.toString();
    }

    public static UnitConfig getLocationForThing(final ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        if (thingDTO.location != null) {
            List<UnitConfig> locationConfigs = Registries.getUnitRegistry(true).getUnitConfigsByLabelAndUnitType(thingDTO.location, UnitType.LOCATION);

            if (locationConfigs.size() == 0) {
                throw new NotAvailableException("Location[" + thingDTO.location + "] for thing[" + thingDTO + "]");
            }

            return locationConfigs.get(0);
        }
        throw new NotAvailableException("Location of thing[" + thingDTO + "]");
    }

    public static EnrichedThingDTO getThingForDevice(final UnitConfig deviceUnitConfig) throws NotAvailableException {
        return OpenHABRestCommunicator.getInstance().getThing(getThingIdFromDevice(deviceUnitConfig));
    }

    public static String getThingIdFromDevice(final UnitConfig deviceUnitConfig) throws NotAvailableException {
        final MetaConfigPool metaConfigPool = new MetaConfigPool();
        metaConfigPool.register(new MetaConfigVariableProvider(deviceUnitConfig.getAlias(0) + MetaConfig.class.getSimpleName(), deviceUnitConfig.getMetaConfig()));

        // get the value for the thing uid key
        return metaConfigPool.getValue(OPENHAB_THING_UID_KEY);
    }

    public static EnrichedThingDTO getThingForUnit(final UnitConfig unitConfig) throws NotAvailableException {
        return OpenHABRestCommunicator.getInstance().getThing(getThingIdForUnit(unitConfig));
    }

    public static String getThingIdForUnit(final UnitConfig unitConfig) {
        return new ThingUID("bco", unitConfig.getUnitType().name().toLowerCase(), unitConfig.getId()).toString();
    }

    public static DeviceClass getDeviceClassByDiscoveryResult(final DiscoveryResultDTO discoveryResult) throws CouldNotPerformException {
        // transform to key value map
        final Map<String, String> properties = new HashMap<>();
        for (Entry<String, Object> stringObjectEntry : discoveryResult.properties.entrySet()) {
            properties.put(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
        }
        // resolve
        return resolveDeviceClass(discoveryResult.label, discoveryResult.thingTypeUID, properties);
    }

    public static DeviceClass getDeviceClassForThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        return resolveDeviceClass(thingDTO.label, thingDTO.thingTypeUID, thingDTO.properties);
    }

    public static GatewayClass getGatewayClassByDiscoveryResult(final DiscoveryResultDTO discoveryResult) throws CouldNotPerformException {
        // transform to key value map
        final Map<String, String> properties = new HashMap<>();
        for (Entry<String, Object> stringObjectEntry : discoveryResult.properties.entrySet()) {
            properties.put(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
        }

        // resolve
        return resolveGatewayClass(discoveryResult.label, discoveryResult.thingTypeUID, properties);
    }

    public static GatewayClass getGatewayClassForThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        return resolveGatewayClass(thingDTO.label, thingDTO.thingTypeUID, thingDTO.properties);
    }

// outdated and can later be removed.
//    private static String getClassIdentifierForBinding(final String thingTypeUID, final Map<String, ?> properties) throws NotAvailableException {
//        if (thingTypeUID.startsWith("zwave")) {
//            final String deviceType = (String) properties.get(ZWAVE_DEVICE_TYPE_KEY);
//            if (deviceType == null) {
//                throw new NotAvailableException("ZWave deviceType");
//            }
//            return ZWAVE_DEVICE_TYPE_KEY + ":" + deviceType;
//        } else if (thingTypeUID.startsWith("hue")) {
//            final String modelId = (String) properties.get(HUE_MODEL_ID_KEY);
//            if (modelId == null) {
//                throw new NotAvailableException("Hue modelId");
//            }
//            return HUE_MODEL_ID_KEY + ":" + modelId;
//        }
//        String[] split = thingTypeUID.split(":");
//        if (split.length >= 2) {
//            return split[1];
//        }
//        return thingTypeUID;
//    }


    private static GatewayClass resolveGatewayClass(final String thingLabel, final String thingTypeUID, final Map<String, String> properties) throws CouldNotPerformException {

        // filter some gateways that are not supported yet
//            case "OPENHAB_THING_CLASS = hue:bridge":
//            case "OPENHAB_THING_CLASS = zwave:serial_zstick":
                // just continue

        // iterate over all gateway classes
        for (final GatewayClass gatewayClass : Registries.getClassRegistry().getGatewayClasses()) {

            // check if the product number already matches
            final String modelId = properties.get(OPENHAB_THING_PROPERTY_KEY_MODEL_ID);
            if(modelId != null && modelId.equalsIgnoreCase(gatewayClass.getProductNumber())) {
                return gatewayClass;
            }

            // get the most global meta config
            final MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new ProtobufVariableProvider(gatewayClass));
            metaConfigPool.register(new MetaConfigVariableProvider("GatewayClassMetaConfig", gatewayClass.getMetaConfig()));

            try {
                // get the value for the openHAB thing class key
                final String[] thingClassKeys = metaConfigPool.getValue(OPENHAB_THING_CLASS_KEY).split(",");
                for (final String thingClassKey : thingClassKeys) {

                    // check thing uid match
                    if (thingTypeUID.equals(thingClassKey)) {
                        return gatewayClass;
                    }

                    // check variable match
                    if (match(thingClassKey.trim(), properties, gatewayClass.getLabel())) {
                        return gatewayClass;
                    }
                }
            } catch (NotAvailableException ex) {
                // value for gateway not available so continue
            }
        }
        // throw exception because gateway class could not be found
        throw new NotAvailableException("Could not resolve any GatewayClass for Thing[" + thingTypeUID + "] with Label[" + thingLabel + "] given the following Properties["+ StringProcessor.transformCollectionToString(properties.entrySet(), stringStringEntry -> stringStringEntry.getKey() + ":" + stringStringEntry.getValue(),", ") +"]");
    }

    private static DeviceClass resolveDeviceClass(final String thingLabel, final String thingTypeUID, final Map<String, String> properties) throws CouldNotPerformException {

        // filter some things that are not supported yet
        switch (thingTypeUID) {
            case "hue:group":
                throw new NotSupportedException(thingLabel, "bco");
            default:
                // just continue
        }

        // iterate over all device classes
        for (final DeviceClass deviceClass : Registries.getClassRegistry().getDeviceClasses()) {

            // check if the product number already matches
            final String modelId = properties.get(OPENHAB_THING_PROPERTY_KEY_MODEL_ID);
            if(modelId != null && modelId.equalsIgnoreCase(deviceClass.getProductNumber())) {
                return deviceClass;
            }

            // get the most global meta config
            final MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new ProtobufVariableProvider(deviceClass));
            metaConfigPool.register(new MetaConfigVariableProvider("DeviceClassMetaConfig", deviceClass.getMetaConfig()));

            try {
                // get the value for the openHAB thing class key
                final String[] thingClassKeys = metaConfigPool.getValue(OPENHAB_THING_CLASS_KEY).split(",");
                for (final String thingClassKey : thingClassKeys) {
                    // check thing uid match
                    if (thingTypeUID.equals(thingClassKey)) {
                        return deviceClass;
                    }

                    // check variable match
                    if (match(thingClassKey.trim(), properties, deviceClass.getLabel())) {
                        return deviceClass;
                    }
                }
            } catch (NotAvailableException ex) {
                // value for device not available so continue
            }
        }
        // throw exception because device class could not be found
        throw new NotAvailableException("Could not resolve any DeviceClass for Thing[" + thingTypeUID + "] with Label[" + thingLabel + "] given the following Properties["+ StringProcessor.transformCollectionToString(properties.entrySet(), stringStringEntry -> stringStringEntry.getKey() + ":" + stringStringEntry.getValue(),", ") +"]");
    }

    private static boolean match(final String thingClassKey, final Map<String, String> properties, final LabelType.Label label) {

        final String[] thingKeyVaulePair = thingClassKey.split(":");
        if (thingKeyVaulePair.length != 2) {
            LOGGER.warn("Invalid thing class key found for "+LabelProcessor.getBestMatch(label, "?")+ "! Class will be ignored.");
            return false;
        }

        // check for matches
        for (Entry<String, String> keyValueEntry : properties.entrySet()) {
            // if key matches than check entry
            if(keyValueEntry.getKey().equalsIgnoreCase(thingKeyVaulePair[0])) {
                if (keyValueEntry.getValue().equalsIgnoreCase(thingKeyVaulePair[1])) {
                    return true;
                }
            }
        }

        // no matches found
        return false;
    }

    public static Map<ServiceType, ServicePattern> generateServiceMap(final UnitConfig unitConfig) {
        // build service mapping for services to create matching items
        // this map will only contain provider and operation services, and if there are both for the same service the operation service will be saved
        final Map<ServiceType, ServicePattern> serviceTypePatternMap = new HashMap<>();
        for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            if (serviceConfig.getServiceDescription().getPattern() == ServicePattern.CONSUMER) {
                continue;
            }

            if (serviceConfig.getServiceDescription().getPattern() == ServicePattern.STREAM) {
                continue;
            }

            if (!serviceTypePatternMap.containsKey(serviceConfig.getServiceDescription().getServiceType())) {
                serviceTypePatternMap.put(serviceConfig.getServiceDescription().getServiceType(), serviceConfig.getServiceDescription().getPattern());
            } else {
                if (serviceTypePatternMap.get(serviceConfig.getServiceDescription().getServiceType()) == ServicePattern.PROVIDER) {
                    if (serviceConfig.getServiceDescription().getPattern() == ServicePattern.OPERATION) {
                        serviceTypePatternMap.put(serviceConfig.getServiceDescription().getServiceType(), serviceConfig.getServiceDescription().getPattern());
                    }
                }
            }
        }
        return serviceTypePatternMap;
    }

    private static String getChannelUID(final UnitConfig unitConfig, final ServiceType serviceType, final ServicePattern servicePattern, final ThingDTO thingDTO) throws CouldNotPerformException {
        String channelUID = "";
        // todo: validate that unit host is available by using UnitConfigProcessor.isHostUnitAvailable(unitConfig) and make sure apps are handled as well.
        final UnitConfig deviceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId());
        final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());
        outer:
        for (final UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
            if (!unitTemplateConfig.getId().equals(unitConfig.getUnitTemplateConfigId())) {
                continue;
            }

            for (final ServiceTemplateConfig serviceTemplateConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                if (serviceTemplateConfig.getServiceType() != serviceType) {
                    continue;
                }

                final MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider("ServiceTemplateConfigMetaConfig", serviceTemplateConfig.getMetaConfig()));
                try {
                    final String channelIdGuess = thingDTO.UID + ":" + metaConfigPool.getValue(OPENHAB_THING_CHANNEL_TYPE_UID_KEY);

                    for (final ChannelDTO channelDTO : thingDTO.channels) {
                        if (channelDTO.uid.equals(channelIdGuess)) {
                            channelUID = channelDTO.uid;
                            break outer;
                        }
                    }
                    LOGGER.warn("Could not resolve channel for id [" + channelIdGuess + "]");
                } catch (NotAvailableException ex) {
                    LOGGER.warn("Service[" + serviceType.name() + "] of unitTemplateConfig[" + unitTemplateConfig.getUnitType().name() +
                            "] deviceClass[" + LabelProcessor.getBestMatch(deviceClass.getLabel()) + "] handled by openHAB app does have a channel configured");
                }
            }
        }

        if (channelUID.isEmpty()) {
            throw new NotAvailableException("ChannelUID for service[" + serviceType.name() + "] of unit[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "]");
        }

        return channelUID;
    }

    public static void registerAndValidateItems(final UnitConfig unitConfig, final ThingDTO thingDTO) throws CouldNotPerformException {
        final List<ItemChannelLinkDTO> itemChannelLinks = OpenHABRestCommunicator.getInstance().getItemChannelLinks();
        for (final Entry<ServiceType, ServicePattern> entry : generateServiceMap(unitConfig).entrySet()) {
            final ServiceType serviceType = entry.getKey();
            final ServicePattern servicePattern = entry.getValue();

            LOGGER.debug("Register/Validate item for service[" + serviceType.name() + "] of unit[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "]");

            String itemName = OpenHABItemProcessor.generateItemName(unitConfig, serviceType);

            String channelUID;
            try {
                channelUID = getChannelUID(unitConfig, serviceType, servicePattern, thingDTO);
            } catch (NotAvailableException ex) {
                LOGGER.warn("Skip service[" + serviceType.name() + ", " + servicePattern.name() + "] of unit[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "] because no channel is available ");
                continue;
            }
            LOGGER.debug("Found according channel uid[" + channelUID + "]");


            if (OpenHABRestCommunicator.getInstance().hasItem(itemName)) {
                // item is already registered so validate that link exists
                boolean hasChannel = false;
                for (ItemChannelLinkDTO itemChannelLink : itemChannelLinks) {
                    if (itemChannelLink.itemName.equals(itemName) && itemChannelLink.channelUID.equals(channelUID)) {
                        hasChannel = true;
                        break;
                    }
                }
                if (!hasChannel) {
                    OpenHABRestCommunicator.getInstance().registerItemChannelLink(itemName, channelUID);
                }
                continue;
            }

            // create and register item
            ItemDTO itemDTO = new ItemDTO();
            itemDTO.label = generateItemLabel(unitConfig, serviceType);
            itemDTO.name = itemName;

            // try to resolve item type from meta config
            // todo: validate that unit host is available by using UnitConfigProcessor.isHostUnitAvailable(unitConfig) and make sure apps are handled as well.
            final UnitConfig deviceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId());
            final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());
            outer:
            for (UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
                if (!unitTemplateConfig.getId().equals(unitConfig.getUnitTemplateConfigId())) {
                    continue;
                }

                for (ServiceTemplateConfig serviceTemplateConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                    if (serviceTemplateConfig.getServiceType() != entry.getKey()) {
                        continue;
                    }

                    MetaConfigVariableProvider variableProvider = new MetaConfigVariableProvider("ServiceTemplateConfigMetaConfig", serviceTemplateConfig.getMetaConfig());
                    try {
                        itemDTO.type = variableProvider.getValue(OPENHAB_ITEM_TYPE_KEY);
                        LOGGER.warn("Found special item type for service {} {}", serviceType.name(), itemDTO.type);
                        break outer;
                    } catch (NotAvailableException ex) {
                        // do nothing because default type will be chosen
                    }
                }
            }
            // get default item type for service type
            if (itemDTO.type == null || itemDTO.type.isEmpty()) {
                try {
                    itemDTO.type = OpenHABItemProcessor.getItemType(serviceType);
                } catch (NotAvailableException ex) {
                    LOGGER.warn("Skip service[" + serviceType.name() + "] of unit[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "] because no item type available");
                    continue;
                }
            }

            final String labelBefore = itemDTO.label;
            itemDTO = OpenHABRestCommunicator.getInstance().registerItem(itemDTO);
            LOGGER.info("Successfully registered item[" + itemDTO.name + "] for dal unit");

            if (!labelBefore.equals(itemDTO.label)) {
                LOGGER.warn("Item label {} changed label from {} to {}", itemDTO.name, labelBefore, itemDTO.label);
            }
            if (deviceClass.getCompany().equalsIgnoreCase("homematic")) {
                itemDTO.label = itemDTO.label + " test";
                itemDTO = OpenHABRestCommunicator.getInstance().updateItem(itemDTO);
                LOGGER.warn("Updated item {} to label {}", itemDTO.label);
            }

            // link item to thing channel
            OpenHABRestCommunicator.getInstance().registerItemChannelLink(itemDTO.name, channelUID);
            LOGGER.debug("Successfully created link between item[" + itemDTO.name + "] and channel[" + channelUID + "]");
        }
    }

    public static String generateItemLabel(final UnitConfig unitConfig, final ServiceType serviceType) throws CouldNotPerformException {
        return LabelProcessor.getBestMatch(unitConfig.getLabel()) + " " + LabelProcessor.getBestMatch(Registries.getTemplateRegistry().getServiceTemplateByType(serviceType).getLabel());
    }

    /**
     * Update the thing label and location of a thing according to a device.
     *
     * @param deviceUnitConfig the device from which the label and location is taken
     * @param thing            the thing which is updated
     * @return if the thing has been updated meaning that the label or location changed
     * @throws CouldNotPerformException if the update could not be performed
     */
    public static boolean updateThingToUnit(final UnitConfig deviceUnitConfig, final EnrichedThingDTO thing) throws CouldNotPerformException {
        boolean modification = false;
        final String label = LabelProcessor.getBestMatch(deviceUnitConfig.getLabel());
        if (thing.label == null || !thing.label.equals(label)) {
            thing.label = label;
            modification = true;
        }

        final UnitConfig locationUnitConfig = Registries.getUnitRegistry().getUnitConfigById(deviceUnitConfig.getPlacementConfig().getLocationId());
        final String locationLabel = LabelProcessor.getBestMatch(locationUnitConfig.getLabel());
        if (thing.location == null || !thing.location.equals(locationLabel)) {
            thing.location = locationLabel;
            modification = true;
        }

        return modification;
    }

    /**
     * Update the unit label and location according to a changed thing.
     *
     * @param thing      the thing which has changed
     * @param unitConfig the unit which is updated
     * @return if the thing has been updated meaning that the label or location changed
     * @throws CouldNotPerformException if the update could not be performed
     */
    public static boolean updateUnitToThing(final EnrichedThingDTO thing, final UnitConfig.Builder unitConfig) throws CouldNotPerformException, InterruptedException {
        boolean modification = false;
        // update label and location
        if (!LabelProcessor.contains(unitConfig.getLabel(), thing.label)) {
            modification = true;
            String oldLabel = LabelProcessor.getBestMatch(unitConfig.getLabel());
            LabelProcessor.replace(unitConfig.getLabelBuilder(), oldLabel, thing.label);
        }
        if (thing.location != null) {
            String locationId;
            try {
                locationId = SynchronizationProcessor.getLocationForThing(thing).getId();
            } catch (NotAvailableException ex) {
                LOGGER.info("Skip setting location of unit for thing {} because its location {} is not available", thing.UID, thing.location);
                return modification;
            }

            if (!locationId.equals(unitConfig.getPlacementConfig().getLocationId())) {
                modification = true;
                unitConfig.getPlacementConfigBuilder().setLocationId(locationId);
                if (unitConfig.getUnitType() == UnitType.DEVICE) {
                    unitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setLocationId(locationId).setTimestamp(TimestampProcessor.getCurrentTimestamp());
                }
            }
        }

        return modification;
    }

    /**
     * Helper method for deleting a thing. This method will also delete all items and itemChannelLinks connected
     * to the thing.
     *
     * @param thingDTO the thing to be removed.
     * @throws CouldNotPerformException if removing the thing fails.
     */
    public static void deleteThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        final List<ItemChannelLinkDTO> itemChannelLinks = OpenHABRestCommunicator.getInstance().getItemChannelLinks();
        for (final ChannelDTO channel : thingDTO.channels) {
            for (final ItemChannelLinkDTO itemChannelLink : itemChannelLinks) {
                if (!itemChannelLink.channelUID.equals(channel.uid)) {
                    continue;
                }

                if (OpenHABRestCommunicator.getInstance().hasItem(itemChannelLink.itemName)) {
                    OpenHABRestCommunicator.getInstance().deleteItem(itemChannelLink.itemName);
                }

                OpenHABRestCommunicator.getInstance().deleteItemChannelLink(itemChannelLink);
            }
        }
        OpenHABRestCommunicator.getInstance().deleteThing(thingDTO.UID);
    }
}
