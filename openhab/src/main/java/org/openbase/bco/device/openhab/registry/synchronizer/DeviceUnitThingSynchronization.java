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

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.device.openhab.OpenHABRestCommunicator;
import org.openbase.bco.registry.clazz.core.consistency.KNXDeviceClassConsistencyHandler;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Class managing the synchronization from BCO device units to openHAB things.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceUnitThingSynchronization extends AbstractSynchronizer<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    private static final String BINDING_ID_KNX = "knx";
    private static final String THING_TYPE_KNX = "device";
    private static final String CHANNEL_KIND = "STATE";
    private static final String KEY_GROUP_ADDRESS = "ga";
    private static final String KEY_POSITION = "position";
    private static final String KEY_SWITCH = "switch";
    private static final String KEY_STOP_MOVE = "stopMove";
    private static final String KEY_UP_DOWN = "upDown";
    private static final String KEY_OPENHAB_BINDING_CONFIG = "OPENHAB_BINDING_CONFIG";

    /**
     * Create a new synchronization test from BCO device units to openHAB things.
     *
     * @param synchronizationLock a lock used during a synchronization. This is lock should be shared with
     *                            other synchronization managers so that they will not interfere with each other.
     *
     * @throws InstantiationException if instantiation fails
     * @throws NotAvailableException  if the unit registry is not available
     */
    public DeviceUnitThingSynchronization(final SyncObject synchronizationLock) throws InstantiationException, NotAvailableException {
        super(Registries.getUnitRegistry().getDeviceUnitConfigRemoteRegistry(false), synchronizationLock);
    }

    /**
     * Update the label and location of a thing belonging to the device unit.
     *
     * @param identifiableMessage the device unit updated
     *
     * @throws CouldNotPerformException if the thing could not be updated
     */
    @Override
    public void update(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.trace("Synchronize update {} ...", identifiableMessage);
        // update thing label and location if needed
        final UnitConfig deviceUnitConfig = identifiableMessage.getMessage();
        try {
            final EnrichedThingDTO thing = SynchronizationProcessor.getThingForDevice(deviceUnitConfig);
            if (SynchronizationProcessor.updateThingToUnit(deviceUnitConfig, thing)) {
                OpenHABRestCommunicator.getInstance().updateThing(thing);
            }
        } catch (NotAvailableException ex) {
            // do nothing because thing for device could not be found
        }
    }

    /**
     * Only perform an initial sync between a device unit and its thing by calling {@link #update(IdentifiableMessage)}
     * if its the initial sync.
     *
     * @param identifiableMessage the device unit initially registered
     *
     * @throws CouldNotPerformException if the initial update could not be performed.
     */
    @Override
    public void register(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException, InterruptedException {
        logger.trace("Synchronize registration {} ...", identifiableMessage);
        // if this is the initial sync make sure that things and devices are synced
        if (isInitialSync()) {
            update(identifiableMessage);
        }

        // register thing for knx device
        final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(identifiableMessage.getMessage().getDeviceConfig().getDeviceClassId());
        if (!KNXDeviceClassConsistencyHandler.isKNXDeviceClass(deviceClass)) {
            return;
        }
        try {
            // validate if thing already registered
            SynchronizationProcessor.getThingForDevice(identifiableMessage.getMessage());
        } catch (NotAvailableException ex) {
            // thing not yet registered
            registerKNXThings(identifiableMessage.getMessage());
        }
    }

    /**
     * Remove the thing belonging to a device unit. This is skipped if the thing is not available, e.g. because
     * it has already been removed, or if another device still manages the thing.
     * If another device manages the thing, the thing and its items will be updated. This is necessary for a
     * synchronization with an old BCO registry.
     *
     * @param identifiableMessage the removed device unit
     *
     * @throws CouldNotPerformException if the thing could not be removed or updated
     */
    @Override
    public void remove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.trace("Synchronize removal {} ...", identifiableMessage);
        // remove thing belonging to the device
        final UnitConfig deviceUnitConfig = identifiableMessage.getMessage();

        // retrieve thing
        EnrichedThingDTO thing;
        try {
            thing = SynchronizationProcessor.getThingForDevice(deviceUnitConfig);
        } catch (NotAvailableException ex) {
            // do nothing because thing is already removed or never existed
            return;
        }

        // if there is another device corresponding to this thing, do not delete it because the other device has taken over
        for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.DEVICE)) {
            try {
                // check if another device has the same thing id
                String thingId = SynchronizationProcessor.getThingIdFromDevice(unitConfig);
                if (thingId.equals(thing.UID)) {
                    // perform update for other device
                    if (SynchronizationProcessor.updateThingToUnit(unitConfig, thing)) {
                        OpenHABRestCommunicator.getInstance().updateThing(thing);
                        for (final String unitId : unitConfig.getDeviceConfig().getUnitIdList()) {
                            SynchronizationProcessor.registerAndValidateItems(Registries.getUnitRegistry().getUnitConfigById(unitId), thing);
                        }
                    }
                    return;
                }
            } catch (NotAvailableException ex) {
                // device is not an openHAB device so do nothing
            }
        }

        // delete thing
        SynchronizationProcessor.deleteThing(thing);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list of device units managed by the unit registry
     *
     * @throws CouldNotPerformException it the device units are not available
     */
    @Override
    public List<IdentifiableMessage<String, UnitConfig, Builder>> getEntries() throws CouldNotPerformException {
        return Registries.getUnitRegistry().getDeviceUnitConfigRemoteRegistry(true).getEntries();
    }

    /**
     * Verify that the device is managed by openHAB.
     *
     * @param identifiableMessage the device unit checked
     *
     * @return if the device managed by openHAB
     */
    @Override
    public boolean isSupported(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        // validate that the device is configured via openHAB
//        try {
//            SynchronizationProcessor.getThingIdFromDevice(identifiableMessage.getMessage());
//            return true;
//        } catch (NotAvailableException ex) {
//            return false;
//        }
        try {
            DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(identifiableMessage.getMessage().getDeviceConfig().getDeviceClassId());
            if (deviceClass.getBindingConfig().getBindingId().equalsIgnoreCase("openhab")) {
                return true;
            }
        } catch (CouldNotPerformException ex) {
            if(!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(new InvalidStateException("Not supported check failed!", ex), logger);
            }
        }
        return false;
    }

    @Override
    protected void afterInternalSync() {
        logger.info("Internal sync finished!");
    }

    private void registerKNXThings(final UnitConfig deviceUnitConfig) throws CouldNotPerformException, InterruptedException {
        String bridgeUID = null;
        for (final EnrichedThingDTO thing : OpenHABRestCommunicator.getInstance().getThings()) {
            if (thing.UID != null && thing.UID.startsWith(BINDING_ID_KNX) && (thing.bridgeUID == null || thing.bridgeUID.isEmpty())) {
                bridgeUID = thing.UID;
                break;
            }
        }

        if (bridgeUID == null) {
            throw new NotAvailableException("Thing for knx bridge");
        }

        final DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());

        logger.info("Create thing for knx device {}", deviceUnitConfig.getAlias(0));
        final UnitConfig.Builder device = deviceUnitConfig.toBuilder();

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

        for (final String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
            final UnitConfig dalUnitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
            UnitTemplateConfig templateConfig = null;
            for (UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
                if (dalUnitConfig.getUnitTemplateConfigId().equals(unitTemplateConfig.getId())) {
                    templateConfig = unitTemplateConfig;
                    break;
                }
            }

            if (templateConfig == null) {
                logger.error("Could not find unit template config {} of unit {} in device class {}", dalUnitConfig.getUnitTemplateConfigId(), dalUnitConfig.getAlias(0), LabelProcessor.getBestMatch(deviceClass.getLabel()));
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
                    logger.error("Could find service matching template {}", serviceTemplateConfig.getServiceType().name());
                    continue;
                }

                final MetaConfigVariableProvider serviceConfigVariableProvider = new MetaConfigVariableProvider("ServiceConfigBindingConfigMetaConfig", service.getBindingConfig().getMetaConfig());
                final MetaConfigVariableProvider serviceTemplateVariableProvier = new MetaConfigVariableProvider("ServiceTemplateMetaConfig", serviceTemplateConfig.getMetaConfig());

                final String channelId;
                try {
                    channelId = serviceTemplateVariableProvier.getValue(SynchronizationProcessor.OPENHAB_THING_CHANNEL_TYPE_UID_KEY);
                } catch (NotAvailableException e) {
                    logger.error("Skip service {} of unit {} because channel type not defined", service.getServiceDescription().getServiceType(), dalUnitConfig.getAlias(0));
                    continue;
                }

                final String bindingConfig;
                try {
                    bindingConfig = serviceConfigVariableProvider.getValue(KEY_OPENHAB_BINDING_CONFIG);
                } catch (NotAvailableException e) {
                    logger.error("Skip channel {} because binding config for service {} of unit {} not available", channelId, service.getServiceDescription().getServiceType(), dalUnitConfig.getAlias(0));
                    continue;
                }

                final String itemType;
                try {
                    itemType = OpenHABItemProcessor.getItemType(serviceTemplateConfig.getServiceType());
                } catch (NotAvailableException e) {
                    logger.warn("Skip service type {} because item type not available", service.getServiceDescription().getServiceType());
                    continue;
                }

                final ChannelDTO channelDTO = new ChannelDTO();

                ChannelUID channelUID = new ChannelUID(thingUID, channelId);
                channelDTO.id = channelId;
                channelDTO.uid = channelUID.toString();
                ChannelTypeUID channelTypeUID;
                switch (serviceTemplateConfig.getServiceType()) {
                    case BRIGHTNESS_STATE_SERVICE:
                        channelTypeUID = new ChannelTypeUID(BINDING_ID_KNX, "dimmer");
                        break;
                    case POWER_STATE_SERVICE:
                    case BUTTON_STATE_SERVICE:
                        channelTypeUID = new ChannelTypeUID(BINDING_ID_KNX, "switch");
                        break;
                    case BLIND_STATE_SERVICE:
                        channelTypeUID = new ChannelTypeUID(BINDING_ID_KNX, "rollershutter");
                        break;
                    case POWER_CONSUMPTION_STATE_SERVICE:
                    case TEMPERATURE_STATE_SERVICE:
                    case TARGET_TEMPERATURE_STATE_SERVICE:
                        channelTypeUID = new ChannelTypeUID(BINDING_ID_KNX, "number");
                        break;
                    default:
                        logger.warn("Skip service type {} because knx channel type unknown", service.getServiceDescription().getServiceType());
                        continue;
                }
                channelDTO.channelTypeUID = channelTypeUID.toString();
                channelDTO.label = StringProcessor.transformToPascalCase(channelId);
                channelDTO.itemType = itemType;
                channelDTO.kind = CHANNEL_KIND;
                channelDTO.configuration = new HashMap<>();
                channelDTO.properties = new HashMap<>();

                switch (dalUnitConfig.getUnitType()) {
                    case DIMMABLE_LIGHT:
                    case DIMMER:
                        switch (serviceTemplateConfig.getServiceType()) {
                            case BRIGHTNESS_STATE_SERVICE:
                                channelDTO.configuration.put(KEY_POSITION, bindingConfig);
                                break;
                            case POWER_STATE_SERVICE:
                                channelDTO.configuration.put(KEY_SWITCH, bindingConfig);
                                break;
                        }
                        break;
                    default:
                        switch (serviceTemplateConfig.getServiceType()) {
                            case BLIND_STATE_SERVICE:
                                String[] configs = bindingConfig.replace(" ", "").split(",");
                                if (configs.length != 3) {
                                    logger.warn("Cannot interpret config {} for blind state. Expected three group addresses separated by comma, e.g.: 2/2/0, 2/2/1, 2/2/2+2/2/3");
                                    continue;
                                }
                                channelDTO.configuration.put(KEY_UP_DOWN, configs[0]);
                                channelDTO.configuration.put(KEY_STOP_MOVE, configs[1]);
                                channelDTO.configuration.put(KEY_POSITION, configs[2]);
                                break;
                            default:
                                channelDTO.configuration.put(KEY_GROUP_ADDRESS, bindingConfig);
                                break;
                        }
                }

                channelDTO.defaultTags = new HashSet<>();
                channelDTO.description = "";
                thingDTO.channels.add(channelDTO);
            }
        }

        Entry.Builder entry = device.getMetaConfigBuilder().addEntryBuilder();
        entry.setKey(SynchronizationProcessor.OPENHAB_THING_UID_KEY);
        entry.setValue(thingDTO.UID);
        try {
            Registries.getUnitRegistry().updateUnitConfig(device.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not update knx device " + device.getAlias(0), ex);
        }

        OpenHABRestCommunicator.getInstance().registerThing(thingDTO);
    }
}
