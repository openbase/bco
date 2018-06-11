package org.openbase.bco.app.openhab.registry.synchronizer;

import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.registry.diff.IdentifiableEnrichedThingDTO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.InventoryStateType.InventoryState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

public class ThingDeviceUnitSynchronizer extends AbstractSynchronizer<String, IdentifiableEnrichedThingDTO> {

    public static final String OPENHAB_THING_UID_KEY = "OPENHAB_THING_UID";
    public static final String OPENHAB_THING_CLASS_KEY = "OPENHAB_THING_CLASS";
    public static final String OPENHAB_THING_CHANNEL_TYPE_UID_KEY = "OPENHAB_THING_CHANNEL_TYPE_UID";

    public ThingDeviceUnitSynchronizer() throws InstantiationException {
        super(new ThingObservable());
    }

    @Override
    public void update(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        final EnrichedThingDTO updatedThing = identifiableEnrichedThingDTO.getDTO();

        // get device unit config for the thing
        final UnitConfig.Builder deviceUnitConfig = getDeviceForThing(updatedThing).toBuilder();

        // TODO: only adding the label, or remove the old one, or at least move new one to higher priority?
        // update label and location
        LabelProcessor.addLabel(deviceUnitConfig.getLabelBuilder(), Locale.ENGLISH, updatedThing.label);
        if (updatedThing.location != null) {
            final String locationId = getLocationForThing(updatedThing).getId();

            deviceUnitConfig.getPlacementConfigBuilder().setLocationId(locationId);
            deviceUnitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setLocationId(locationId).setTimestamp(TimestampProcessor.getCurrentTimestamp());
        }

        try {
            //TODO: add timeout
            Registries.getUnitRegistry().updateUnitConfig(deviceUnitConfig.build()).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not update device[" + deviceUnitConfig.getLabel() + "] for thing[" + identifiableEnrichedThingDTO.getId() + "]", ex);
        }
    }

    @Override
    public void register(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        final ThingDTO thingDTO = identifiableEnrichedThingDTO.getDTO();

        // handle initial sync
        try {
            //TODO: dal unit of the device should also be checked
            getDeviceForThing(thingDTO);
            return;
        } catch (NotAvailableException ex) {
            // do nothing
        }

        //TODO: should this whole action be rolled back if one part fails?
        // get device class for thing
        final DeviceClass deviceClass = getDeviceClassByThing(thingDTO);

        // create device for this class
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
        unitConfig.setUnitType(UnitType.DEVICE);
        unitConfig.getDeviceConfigBuilder().setDeviceClassId(deviceClass.getId());
        unitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setValue(State.INSTALLED).setTimestamp(TimestampProcessor.getCurrentTimestamp());

        // update location according to thing
        if (thingDTO.location != null) {
            String locationId = getLocationForThing(thingDTO).getId();

            //TODO: add locations flat under root location if not available
            unitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setLocationId(locationId);
            unitConfig.getPlacementConfigBuilder().setLocationId(locationId);
        } else {
            logger.info("Thing has no location defined so it will be added at the root location");
        }

        //TODO: which language to use
        // update label according to thing
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, thingDTO.label);

        // add thing uid to meta config to have a mapping between thing and device
        unitConfig.getMetaConfigBuilder().addEntryBuilder().setKey(OPENHAB_THING_UID_KEY).setValue(thingDTO.UID);

        try {
            UnitConfig deviceUnitConfig = Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get();

            // create items for dal units of the device
            registerItems(deviceUnitConfig, thingDTO);
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register device for thing[" + thingDTO.thingTypeUID + "]", ex);
        }
    }

    @Override
    public void remove(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        // TODO: Should a device really be removed if it is removed from openHAB?
        final EnrichedThingDTO enrichedThingDTO = identifiableEnrichedThingDTO.getDTO();

        // get device unit config for thing
        final UnitConfig deviceUnitConfig = getDeviceForThing(enrichedThingDTO);

        // remove device
        try {
            //TODO: add timeout
            Registries.getUnitRegistry().removeUnitConfig(deviceUnitConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not remove device[" + deviceUnitConfig.getLabel() + "] for thing[" + identifiableEnrichedThingDTO.getId() + "]", ex);
        }
    }

    @Override
    public List<IdentifiableEnrichedThingDTO> getEntries() throws CouldNotPerformException {
        final List<IdentifiableEnrichedThingDTO> identifiableEnrichedThingDTOList = new ArrayList<>();
        for (final EnrichedThingDTO enrichedThingDTO : OpenHABRestCommunicator.getInstance().getThings()) {
            identifiableEnrichedThingDTOList.add(new IdentifiableEnrichedThingDTO(enrichedThingDTO));
        }
        return identifiableEnrichedThingDTOList;
    }

    @Override
    public boolean verifyEntry(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) {
        return true;
    }

    /**
     * @param thingDTO
     * @return
     * @throws NotAvailableException
     * @throws InterruptedException  TODO: remove interrupted when removed from registry
     */
    private UnitConfig getDeviceForThing(final ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        // iterate over all devices
        for (final UnitConfig deviceUnitConfig : Registries.getUnitRegistry(true).getUnitConfigs(UnitType.DEVICE)) {
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

    private UnitConfig getLocationForThing(final ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        if (thingDTO.location != null) {
            List<UnitConfig> locationConfigs = Registries.getUnitRegistry(true).getUnitConfigsByLabelAndUnitType(thingDTO.location, UnitType.LOCATION);

            if (locationConfigs.size() == 0) {
                throw new NotAvailableException("Location[" + thingDTO.location + "] for thing[" + thingDTO + "]");
            }

            return locationConfigs.get(0);
        }
        throw new NotAvailableException("Location of thing[" + thingDTO + "]");
    }

    private void registerItems(final UnitConfig deviceUnitConfig, final ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        for (final String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
            final UnitConfig dalUnitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);

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
                logger.info("Register item for service[" + serviceType.name() + "] of unit[" + dalUnitConfig.getAlias(0) + "]");

                String channelUID = "";
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
                            String channelTypeUID = metaConfigPool.getValue(OPENHAB_THING_CHANNEL_TYPE_UID_KEY);

                            for (final ChannelDTO channelDTO : thingDTO.channels) {
                                if (channelDTO.channelTypeUID.equals(channelTypeUID)) {
                                    channelUID = channelDTO.uid;
                                    break outer;
                                }
                            }
                        } catch (NotAvailableException ex) {
                            logger.warn("Service[" + serviceType.name() + "] of unitTemplateConfig[" + unitTemplateConfig.getType().name() + "] deviceClass[" + deviceClass.getLabel() + "] handled by openHAB app does have a channel configured");
                        }
                    }
                }

                if (channelUID.isEmpty()) {
                    throw new NotAvailableException("ChannelUID for service[" + serviceType.name() + "] of unit[" + dalUnitConfig.getAlias(0) + "]");
                }

                // create and register item
                ItemDTO itemDTO = new ItemDTO();
                itemDTO.label = LabelProcessor.getFirstLabel(dalUnitConfig.getLabel());
                itemDTO.name = OpenHABItemHelper.generateItemName(dalUnitConfig, serviceType);
                itemDTO.type = OpenHABItemHelper.getItemType(serviceType, servicePattern);
                itemDTO = OpenHABRestCommunicator.getInstance().registerItem(itemDTO);
                logger.info("Successfully registered item[" + itemDTO.name + "] for dal unit");

                // link item to thing channel
                OpenHABRestCommunicator.getInstance().registerItemChannelLink(itemDTO.name, channelUID);
                logger.info("Successfully created link between item[" + itemDTO.name + "] and channel[" + channelUID + "]");
            }


        }
    }

    private DeviceClass getDeviceClassByThing(final ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        // iterate over all device classes
        for (final DeviceClass deviceClass : Registries.getClassRegistry(true).getDeviceClasses()) {
            // get the most global meta config
            final MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider("DeviceClassMetaConfig", deviceClass.getMetaConfig()));

            try {
                // get the value for the openHAB thing class key
                String thingUID = metaConfigPool.getValue(OPENHAB_THING_CLASS_KEY);
                // if the uid starts with that return the according device class
                if (thingDTO.UID.startsWith(thingUID)) {
                    return deviceClass;
                }
            } catch (NotAvailableException ex) {
                // value for device not available so continue
            }
        }
        // throw exception because device class could not be found
        throw new NotAvailableException("DeviceClass for thing[" + thingDTO.UID + "]");

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
}
