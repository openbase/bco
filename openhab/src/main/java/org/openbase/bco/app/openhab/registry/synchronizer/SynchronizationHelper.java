package org.openbase.bco.app.openhab.registry.synchronizer;

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

import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SynchronizationHelper {

    public static final String ZWAVE_DEVICE_ID_KEY = "zwave_deviceid";

    public static final String OPENHAB_THING_UID_KEY = "OPENHAB_THING_UID";
    public static final String OPENHAB_THING_CLASS_KEY = "OPENHAB_THING_CLASS";
    public static final String OPENHAB_THING_CHANNEL_TYPE_UID_KEY = "OPENHAB_THING_CHANNEL_TYPE_UID";

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationHelper.class);

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
        for (final UnitConfig deviceUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.DEVICE)) {
            // get the most global meta config
            final MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider("UnitMetaConfig", deviceUnitConfig.getMetaConfig()));

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


    public static DeviceClass getDeviceClassByThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        String classIdentifier = thingDTO.thingTypeUID;
        if (thingDTO.thingTypeUID.startsWith("zwave")) {
            classIdentifier = ZWAVE_DEVICE_ID_KEY + ":" + thingDTO.properties.get("zwave_deviceid");
        }
        return getDeviceClassByThing(classIdentifier);
    }

    public static DeviceClass getDeviceClassByThing(final String classIdentifier) throws CouldNotPerformException {
        // iterate over all device classes
        for (final DeviceClass deviceClass : Registries.getClassRegistry().getDeviceClasses()) {
            // get the most global meta config
            final MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider("DeviceClassMetaConfig", deviceClass.getMetaConfig()));

            try {
                // get the value for the openHAB thing class key
                String thingUID = metaConfigPool.getValue(OPENHAB_THING_CLASS_KEY);
                // if the uid starts with that return the according device class
                if (classIdentifier.equalsIgnoreCase(thingUID)) {
                    return deviceClass;
                }
            } catch (NotAvailableException ex) {
                // value for device not available so continue
            }
        }
        // throw exception because device class could not be found
        throw new NotAvailableException("DeviceClass for class identifier[" + classIdentifier + "]");

        //TODO: this is a possible fallback solution which is only tested for the fibaro motion sensor: remove or keep?
//        final String[] thingTypeUIDSplit = thingDTO.thingTypeUID.split(":");
//        if (thingTypeUIDSplit.length != 2) {
//            throw new CouldNotPerformException("Could not parse thingTypeUID[" + thingDTO.thingTypeUID + "]. Does not match known pattern binding:deviceInfo");
//        }
//
//        // String binding = split[0];
//        final String deviceInfo = thingTypeUIDSplit[1];
//        final String[] deviceInfoSplit = deviceInfo.split("_");
//        if (deviceInfoSplit.length < 2) {
//            throw new CouldNotPerformException("Could not parse deviceInfo[" + deviceInfo + "]. Does not match known pattern company_productNumber_...");
//        }
//
//        final String company = deviceInfoSplit[0];
//        final String productNumber = deviceInfoSplit[1];
//
//        try {
//            for (final DeviceClass deviceClass : Registries.getDeviceRegistry().getDeviceClasses()) {
//                if (deviceClass.getCompany().equalsIgnoreCase(company)) {
//                    if (deviceClass.getProductNumber().replace("-", "").equalsIgnoreCase(productNumber)) {
//                        return deviceClass;
//                    }
//                }
//            }
//        } catch (InterruptedException ex) {
//            Thread.currentThread().interrupt();
//            throw new CouldNotPerformException("Interrupted", ex);
//        }
//        throw new NotAvailableException("Device from company[" + company + "] with productNumber[" + productNumber + "]");
    }

    public static void registerAndValidateItems(final UnitConfig dalUnitConfig) throws CouldNotPerformException {
        registerAndValidateItems(dalUnitConfig, getThingForDevice(Registries.getUnitRegistry().getUnitConfigById(dalUnitConfig.getUnitHostId())));
    }

    public static void registerAndValidateItems(final UnitConfig dalUnitConfig, final ThingDTO thingDTO) throws CouldNotPerformException {
        // build service mapping for services to create matching items
        // this map will only contain provider and operation services, and if there are both for the same service the operation service will be saved
        final Map<ServiceType, ServicePattern> serviceTypePatternMap = new HashMap<>();
        for (ServiceConfig serviceConfig : dalUnitConfig.getServiceConfigList()) {
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

        for (final Entry<ServiceType, ServicePattern> entry : serviceTypePatternMap.entrySet()) {
            final ServiceType serviceType = entry.getKey();
            final ServicePattern servicePattern = entry.getValue();

            LOGGER.info("Register/Validate item for service[" + serviceType.name() + "] of unit[" + dalUnitConfig.getAlias(0) + "]");

            String channelUID = "";
            final UnitConfig deviceUnitConfig = Registries.getUnitRegistry().getUnitConfigById(dalUnitConfig.getUnitHostId());
            final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());
            outer:
            for (final UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
                if (!unitTemplateConfig.getId().equals(dalUnitConfig.getUnitTemplateConfigId())) {
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
                        LOGGER.warn("Service[" + serviceType.name() + "] of unitTemplateConfig[" + unitTemplateConfig.getType().name() +
                                "] deviceClass[" + LabelProcessor.getBestMatch(deviceClass.getLabel()) + "] handled by openHAB app does have a channel configured");
                    }
                }
            }

            if (channelUID.isEmpty()) {
                LOGGER.warn("ChannelUID for service[" + serviceType.name() + "] of unit[" + dalUnitConfig.getAlias(0) + "] is not available");
                continue;
            }

            if (OpenHABRestCommunicator.getInstance().hasItem(OpenHABItemHelper.generateItemName(dalUnitConfig, serviceType))) {
                // item is already registered
                continue;
            }

            // create and register item
            ItemDTO itemDTO = new ItemDTO();
            itemDTO.label = LabelProcessor.getFirstLabel(dalUnitConfig.getLabel());
            itemDTO.name = OpenHABItemHelper.generateItemName(dalUnitConfig, serviceType);
            itemDTO.type = OpenHABItemHelper.getItemType(serviceType, servicePattern);
            itemDTO = OpenHABRestCommunicator.getInstance().registerItem(itemDTO);
            LOGGER.info("Successfully registered item[" + itemDTO.name + "] for dal unit");

            // link item to thing channel
            OpenHABRestCommunicator.getInstance().registerItemChannelLink(itemDTO.name, channelUID);
            LOGGER.info("Successfully created link between item[" + itemDTO.name + "] and channel[" + channelUID + "]");
        }
    }
}
