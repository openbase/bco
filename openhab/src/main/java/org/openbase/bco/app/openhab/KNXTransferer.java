package org.openbase.bco.app.openhab;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTOMapper;
import org.openbase.bco.app.openhab.jp.JPOpenHABURI;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemProcessor;
import org.openbase.bco.app.openhab.registry.synchronizer.SynchronizationProcessor;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType.Entry;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class KNXTransferer {

    private static final String BINDING_ID_KNX = "knx";
    private static final String THING_TYPE_KNX = "device";
    private static final String CHANNEL_KIND = "STATE";
    private static final String KEY_PROPERTIES = "properties";
    private static final String KEY_GROUP_ADDRESS = "ga";
    private static final String KEY_POSITION = "position";
    private static final String KEY_SWITCH = "switch";
    private static final String KEY_STOP_MOVE = "stopMove";
    private static final String KEY_UP_DOWN = "upDown";

    private static final String KEY_OPENHAB_BINDING_TYPE = "OPENHAB_BINDING_TYPE";
    private static final String KEY_OPENHAB_BINDING_CONFIG = "OPENHAB_BINDING_CONFIG";

    private static final Logger LOGGER = LoggerFactory.getLogger(KNXTransferer.class);

    public static void main(String[] args) {
        JPService.registerProperty(JPOpenHABURI.class, URI.create("http://localhost:8080"));
        JPService.parseAndExitOnError(args);

        try {
            Registries.waitForData();
            SessionManager.getInstance().login(Registries.getUnitRegistry().getUserUnitIdByUserName("admin"), "admin");

            registerThings();
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
            System.exit(1);
        }
        System.exit(0);
    }

    private static boolean isKNXDeviceClass(final DeviceClass deviceClass) {
        final MetaConfigVariableProvider metaConfigVariableProvider = new MetaConfigVariableProvider("DeviceClassBindingConfig", deviceClass.getBindingConfig().getMetaConfig());

        try {
            return metaConfigVariableProvider.getValue(KEY_OPENHAB_BINDING_TYPE).equals(BINDING_ID_KNX);
        } catch (NotAvailableException ex) {
            return false;
        }
    }

    private static void updateDeviceClasses() throws CouldNotPerformException, InterruptedException, ExecutionException {
        Registries.waitForData();
        for (final DeviceClass deviceClass : Registries.getClassRegistry().getDeviceClasses()) {
            // ignore non knx devices
            if (!isKNXDeviceClass(deviceClass)) {
                continue;
            }

            //TODO: test if adaption of class is needed at all

            final DeviceClass.Builder deviceClassBuilder = deviceClass.toBuilder();

            final Map<ServiceType, Integer> serviceTypeNumberMap = new HashMap<>();
            for (final UnitTemplateConfig.Builder unitTemplateConfig : deviceClassBuilder.getUnitTemplateConfigBuilderList()) {
                for (final ServiceTemplateConfig.Builder serviceTemplateConfig : unitTemplateConfig.getServiceTemplateConfigBuilderList()) {
                    if (!serviceTypeNumberMap.containsKey(serviceTemplateConfig.getServiceType())) {
                        serviceTypeNumberMap.put(serviceTemplateConfig.getServiceType(), 0);
                    }
                    serviceTypeNumberMap.put(serviceTemplateConfig.getServiceType(), serviceTypeNumberMap.get(serviceTemplateConfig.getServiceType()) + 1);

                    Entry.Builder entry = serviceTemplateConfig.getMetaConfigBuilder().addEntryBuilder();
                    entry.setKey(SynchronizationProcessor.OPENHAB_THING_CHANNEL_TYPE_UID_KEY);
                    entry.setValue(serviceTemplateConfig.getServiceType().name().toLowerCase() + serviceTypeNumberMap.get(serviceTemplateConfig.getServiceType()));
                }
            }

            Registries.getClassRegistry().updateDeviceClass(deviceClassBuilder.build()).get();
        }
    }

    private static void registerThings() throws CouldNotPerformException, InterruptedException, ExecutionException {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        OpenHABRestCommunicator.getInstance().waitForOpenHAB();
//        String bridgeUID = null;
//        for (final EnrichedThingDTO thing : OpenHABRestCommunicator.getInstance().getThings()) {
//            if (thing.UID.startsWith(BINDING_ID_KNX) && thing.bridgeUID == null || thing.bridgeUID.isEmpty()) {
//                bridgeUID = thing.UID;
//                break;
//            }
//        }
//
//        if (bridgeUID == null) {
//            throw new NotAvailableException("Thing for knx bridge");
//        }

        String bridgeUID = "knx:ip:1234";
        LOGGER.info("Found bridge {}", bridgeUID);

        Registries.waitForData();
        for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.DEVICE)) {
            final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(unitConfig.getDeviceConfig().getDeviceClassId());
            if (!isKNXDeviceClass(deviceClass)) {
                continue;
            }

            LOGGER.info("Create thing for device {}", unitConfig.getAlias(0));
            // todo: test if thing for device already exists
            //TODO add thing uid in meta config of device
            final UnitConfig.Builder device = unitConfig.toBuilder();

            final ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID_KNX, THING_TYPE_KNX);
            final ThingUID thingUID = new ThingUID(thingTypeUID, device.getId());

            final ThingDTO thingDTO = new ThingDTO();
            thingDTO.bridgeUID = bridgeUID;
            thingDTO.thingTypeUID = thingTypeUID.toString();
            thingDTO.UID = thingUID.toString();
            thingDTO.label = LabelProcessor.getBestMatch(device.getLabel());
            thingDTO.location = LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(device.getPlacementConfig().getLocationId()).getLabel());
            thingDTO.channels = new ArrayList<>();
            thingDTO.properties = new HashMap<>();
            thingDTO.configuration = new HashMap<>();

            for (final String unitId : unitConfig.getDeviceConfig().getUnitIdList()) {
                final UnitConfig dalUnitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
                UnitTemplateConfig templateConfig = null;
                for (UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
                    if (dalUnitConfig.getUnitTemplateConfigId().equals(unitTemplateConfig.getId())) {
                        templateConfig = unitTemplateConfig;
                        break;
                    }
                }

                if (templateConfig == null) {
                    LOGGER.error("Could not find unit template config {} of unit {} in device class {}", dalUnitConfig.getUnitTemplateConfigId(), dalUnitConfig.getAlias(0), LabelProcessor.getBestMatch(deviceClass.getLabel()));
                    continue;
                }

                for (final ServiceTemplateConfig serviceTemplateConfig : templateConfig.getServiceTemplateConfigList()) {
                    ServiceConfig service = null;
                    for (final ServiceConfig serviceConfig : dalUnitConfig.getServiceConfigList()) {
                        if (serviceConfig.getServiceDescription().getServiceType() == serviceTemplateConfig.getServiceType()) {
                            if (service == null || serviceConfig.getServiceDescription().getPattern() == ServicePattern.OPERATION) {
                                service = serviceConfig;
                            }
                        }
                    }

                    if (service == null) {
                        LOGGER.error("Could find service matching template {}", serviceTemplateConfig.getServiceType().name());
                        continue;
                    }

                    final MetaConfigVariableProvider serviceConfigVariableProvider = new MetaConfigVariableProvider("ServiceConfigBindingConfigMetaConfig", service.getBindingConfig().getMetaConfig());
                    final MetaConfigVariableProvider serviceTemplateVariableProvier = new MetaConfigVariableProvider("ServiceTemplateMetaConfig", serviceTemplateConfig.getMetaConfig());

                    final String channelId;
                    try {
                        channelId = serviceTemplateVariableProvier.getValue(SynchronizationProcessor.OPENHAB_THING_CHANNEL_TYPE_UID_KEY);
                    } catch (NotAvailableException e) {
                        LOGGER.error("Skip service {} of unit {} because channel type not defined", service.getServiceDescription().getServiceType(), dalUnitConfig.getAlias(0));
                        continue;
                    }

                    final String bindingConfig;
                    try {
                        bindingConfig = serviceConfigVariableProvider.getValue(KEY_OPENHAB_BINDING_CONFIG);
                    } catch (NotAvailableException e) {
                        LOGGER.error("Skip channel {} because binding config for service {} of unit {} not available", channelId, service.getServiceDescription().getServiceType(), dalUnitConfig.getAlias(0));
                        continue;
                    }

                    final String itemType;
                    try {
                        itemType = OpenHABItemProcessor.getItemType(serviceTemplateConfig.getServiceType());
                    } catch (NotAvailableException e) {
                        LOGGER.warn("Skip service type {} because item type not available", service.getServiceDescription().getServiceType());
                        continue;
                    }

                    final ChannelDTO channelDTO = new ChannelDTO();
                    ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID_KNX, channelId);
                    ChannelUID channelUID = new ChannelUID(thingUID, channelId);
                    channelDTO.id = channelId;
                    channelDTO.uid = channelUID.toString();
                    channelDTO.channelTypeUID = channelTypeUID.toString();
                    channelDTO.label = StringProcessor.transformToCamelCase(channelId); //TODO: marian label string generation
                    channelDTO.itemType = itemType;
                    channelDTO.kind = CHANNEL_KIND;
                    channelDTO.configuration = new HashMap<>();
                    //TODO: validate that these properties are used because when the knx binding itself creates thing it adds
                    // another properties map inside the configuration. However, openHAB rejects this if I do it...
                    channelDTO.properties = new HashMap<>();

                    switch (dalUnitConfig.getUnitType()) {
                        case DIMMABLE_LIGHT:
                        case DIMMER:
                            switch (serviceTemplateConfig.getServiceType()) {
                                case BRIGHTNESS_STATE_SERVICE:
                                    channelDTO.properties.put(KEY_POSITION, bindingConfig);
                                    break;
                                case POWER_STATE_SERVICE:
                                    channelDTO.properties.put(KEY_SWITCH, bindingConfig);
                                    break;
                            }
                            break;
                        default:
                            switch (serviceTemplateConfig.getServiceType()) {
                                case BLIND_STATE_SERVICE:
                                    String[] configs = bindingConfig.replace(" ", "").split(",");
                                    if (configs.length != 3) {
                                        LOGGER.warn("Cannot interpret config {} for blind state. Expected three group addresses separated by comma, e.g.: 2/2/0, 2/2/1, 2/2/2+2/2/3");
                                        continue;
                                    }
                                    channelDTO.properties.put(KEY_UP_DOWN, configs[0]);
                                    channelDTO.properties.put(KEY_STOP_MOVE, configs[1]);
                                    channelDTO.properties.put(KEY_POSITION, configs[2]);
                                    break;
                                default:
                                    channelDTO.properties.put(KEY_GROUP_ADDRESS, bindingConfig);
                                    break;
                            }
                    }

                    channelDTO.defaultTags = new HashSet<>();
                    channelDTO.description = "";
                    thingDTO.channels.add(channelDTO);
                }
            }

            LOGGER.info("Register thing:\n{}", gson.toJson(thingDTO));

            OpenHABRestCommunicator.getInstance().registerThing(thingDTO);
            Entry.Builder entry = device.getMetaConfigBuilder().addEntryBuilder();
            entry.setKey(SynchronizationProcessor.OPENHAB_THING_UID_KEY);
            entry.setValue(thingDTO.UID);
            Registries.getUnitRegistry().updateUnitConfig(device.build()).get();
        }
    }
}
