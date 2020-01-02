package org.openbase.bco.registry.clazz.core.consistency;

/*-
 * #%L
 * BCO Registry Class Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass.Builder;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClassOrBuilder;

import java.util.*;

/**
 * Consistency handler generating channelTypeUID entries in the meta config of services provided by a device class.
 * These entries are needed to generate things in openHAB for devices of this class.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class KNXDeviceClassConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceClass, Builder> {

    public static final String BINDING_ID_KNX = "knx";
    public static final String KEY_OPENHAB_BINDING_TYPE = "OPENHAB_BINDING_TYPE";

    private static final String OPENHAB_THING_CHANNEL_TYPE_UID_KEY = "OPENHAB_THING_CHANNEL_TYPE_UID";

    public KNXDeviceClassConsistencyHandler() {
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, DeviceClass, Builder> entry, final ProtoBufMessageMap<String, DeviceClass, Builder> entryMap, final ProtoBufRegistry<String, DeviceClass, Builder> registry) throws CouldNotPerformException, EntryModification {
        // ignore non knx devices
        if (!isKNXDeviceClass(entry.getMessage())) {
            return;
        }

        final DeviceClass.Builder deviceClass = entry.getMessage().toBuilder();
        final String separator = "-";

        // count number of same service types in device class
        final Map<ServiceType, Integer> serviceTypeNumberMap = new HashMap<>();
        // iterate over all service template configs
        for (final UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
            for (final ServiceTemplateConfig serviceTemplateConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                // if no entry in map for service type add
                if (!serviceTypeNumberMap.containsKey(serviceTemplateConfig.getServiceType())) {
                    serviceTypeNumberMap.put(serviceTemplateConfig.getServiceType(), 0);
                }
                // increment number of service types
                serviceTypeNumberMap.put(serviceTemplateConfig.getServiceType(), serviceTypeNumberMap.get(serviceTemplateConfig.getServiceType()) + 1);
            }
        }

        // create a new map with a set of numbers from 1 to the number of service types in the class for each service type
        final Map<ServiceType, Set<Integer>> serviceTypeNumberSetMap = new HashMap<>();
        for (final Map.Entry<ServiceType, Integer> mapEntry : serviceTypeNumberMap.entrySet()) {
            final Set<Integer> numberList = new HashSet<>();
            for (int i = 1; i <= mapEntry.getValue(); i++) {
                numberList.add(i);
            }
            serviceTypeNumberSetMap.put(mapEntry.getKey(), numberList);
        }

        // save all service template configs without a channel configured
        final List<ServiceTemplateConfig.Builder> serviceWithoutChannel = new ArrayList<>();
        // iterate over all service template configs
        for (final UnitTemplateConfig.Builder unitTemplateConfig : deviceClass.getUnitTemplateConfigBuilderList()) {
            for (final ServiceTemplateConfig.Builder serviceTemplateConfig : unitTemplateConfig.getServiceTemplateConfigBuilderList()) {
                final MetaConfigVariableProvider variableProvider =
                        new MetaConfigVariableProvider("ServiceTemplateConfigMetaConfig", serviceTemplateConfig.getMetaConfig());

                String channelTypeUID = "";
                try {
                    // extract channel parameter
                    channelTypeUID = variableProvider.getValue(OPENHAB_THING_CHANNEL_TYPE_UID_KEY);
                    // underscore is not a supported character in openHAB so remove the channel type id,
                    // this is a fix because these configurations where created this way initially
                    if (channelTypeUID.contains("_")) {
                        serviceTemplateConfig.clearMetaConfig();
                        serviceWithoutChannel.add(serviceTemplateConfig);
                        continue;
                    }
                    // split at separator
                    final String[] split = channelTypeUID.split(separator);
                    // extract number at the end of the channelTypeUID
                    final Integer channelNumber = Integer.parseInt(split[split.length - 1]);
                    // remove this number from the set of still available numbers for the service type
                    serviceTypeNumberSetMap.get(serviceTemplateConfig.getServiceType()).remove(channelNumber);
                } catch (NotAvailableException ex) {
                    // service template does not have a channel configured so add it to the list
                    serviceWithoutChannel.add(serviceTemplateConfig);
                } catch (NumberFormatException ex) {
                    // channelUID does not
                    logger.warn("ChannelTypeUID {} of serviceTemplateConfig {} in unitTemplateConfig {} of deviceClass {} does not match the expected format service_type_number", channelTypeUID, serviceTemplateConfig.getServiceType().name(), unitTemplateConfig.getId(), LabelProcessor.getBestMatch(deviceClass.getLabel()));
                }
            }
        }

        // iterate over all service templates without channel configuration and add configuration
        for (final ServiceTemplateConfig.Builder builder : serviceWithoutChannel) {
            // get first available number from set
            final Integer number = serviceTypeNumberSetMap.get(builder.getServiceType()).iterator().next();
            // remove the number from the set
            serviceTypeNumberSetMap.get(builder.getServiceType()).remove(number);
            // add entry to meta config
            Entry.Builder metaConfigEntry = builder.getMetaConfigBuilder().addEntryBuilder();
            // add key and value for channel
            metaConfigEntry.setKey(OPENHAB_THING_CHANNEL_TYPE_UID_KEY);
            metaConfigEntry.setValue(builder.getServiceType().name().toLowerCase().replaceAll("_", separator) + separator + number);
        }

        // test if there were any services without configured channel, and if there were update the device class
        if (serviceWithoutChannel.size() > 0) {
            throw new EntryModification(entry.setMessage(deviceClass, this), this);
        }
    }

    /**
     * Test if the given device class is managed by the knx binding. This is done by testing if the meta config in the
     * binding config of the device class contains the entry: OPENHAB_BINDING_TYPE = knx.
     *
     * @param deviceClass the device class tested.
     *
     * @return if devices for this device class are managed by the knx binding.
     */
    public static boolean isKNXDeviceClass(final DeviceClassOrBuilder deviceClass) {
        final MetaConfigVariableProvider metaConfigVariableProvider =
                new MetaConfigVariableProvider("DeviceClassBindingConfig", deviceClass.getBindingConfig().getMetaConfig());

        try {
            return metaConfigVariableProvider.getValue(KEY_OPENHAB_BINDING_TYPE).equals(BINDING_ID_KNX);
        } catch (NotAvailableException ex) {
            return false;
        }
    }
}
