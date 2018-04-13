package org.openbase.bco.app.openhab;

import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.app.openhab.diff.IdentifiableEnrichedThingDTO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ListDiff;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.configuration.EntryType.Entry;
import rst.domotic.state.InventoryStateType.InventoryState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OpenHABConfigSynchronizer {

    public static String THING_UID_KEY = "OPENHAB_THING_UID";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ListDiff<String, IdentifiableEnrichedThingDTO> thingDiff;

    private final Runnable restPollingService;

    private boolean initialSync;

    public OpenHABConfigSynchronizer() {
        this.thingDiff = new ListDiff<>();
        this.restPollingService = new Runnable() {
            @Override
            public void run() {
                syncThings();
            }
        };
        this.initialSync = true;
    }

    public void init() {

    }

    public void activate() throws CouldNotPerformException {
        try {
            GlobalScheduledExecutorService.scheduleAtFixedRate(restPollingService, 30, 30, TimeUnit.SECONDS);
        } catch (NotAvailableException ex) {
            // NotAvailableException is only thrown if runnable is null which is not the case here
            new FatalImplementationErrorException(this, ex);
        }

        try {
            Registries.waitForData();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        syncThings();
    }

    private void syncThings() {
        //TODO: this is pretty similar to the registry synchronizer... maybe a super class for both is needed
        logger.info("Trigger device sync");
        try {
            final List<EnrichedThingDTO> things = OpenHABRestCommunicator.getInstance().getThings();
            final List<IdentifiableEnrichedThingDTO> identifiableThings = new ArrayList<>();
            for (EnrichedThingDTO enrichedThingDTO : things) {
                identifiableThings.add(new IdentifiableEnrichedThingDTO(enrichedThingDTO));
            }

            thingDiff.diff(identifiableThings);

            // handle new things
            MultiException.ExceptionStack newExceptionStack = null;
            for (IdentifiableEnrichedThingDTO identifiableNewThing : thingDiff.getNewValueMap().values()) {
                final EnrichedThingDTO newThing = identifiableNewThing.getDTO();
                if (initialSync) {
                    try {
                        getDeviceForThing(newThing);
                        logger.info("Device for thing[" + newThing + "] has been found and should be updated");
                        continue;
                    } catch (NotAvailableException ex) {
                        // do nothing because device will be registered
                    }
                }

                try {
                    registerDeviceForThing(newThing);
                } catch (CouldNotPerformException ex) {
                    newExceptionStack = MultiException.push(this, ex, newExceptionStack);
                }
            }

            initialSync = false;

            // handle removed things
            for (IdentifiableEnrichedThingDTO identifiableRemovedThing : thingDiff.getRemovedValueMap().values()) {
                // todo: remove device or should a device never be removed but instead queried if this is correct
            }

            // handle updated tings
            MultiException.ExceptionStack updateExceptionStack = null;
            for (IdentifiableEnrichedThingDTO identifiableUpdatedThing : thingDiff.getUpdatedValueMap().values()) {
                try {
                    final EnrichedThingDTO updatedThing = identifiableUpdatedThing.getDTO();

                    // get unit config from registry
                    final UnitConfig.Builder deviceUnitConfig = getDeviceForThing(updatedThing).toBuilder();

                    // TODO: verify that label and location are the only fields that need to be synced
                    // update fields and update
                    deviceUnitConfig.setLabel(updatedThing.label);
                    if (updatedThing.location != null) {
                        final String locationId = getLocationForThing(updatedThing).getId();

                        deviceUnitConfig.getPlacementConfigBuilder().setLocationId(locationId);
                        deviceUnitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setLocationId(locationId).setTimestamp(TimestampProcessor.getCurrentTimestamp());
                    }
                    //TODO: what to do if this is not successful?
                    Registries.getUnitRegistry().updateUnitConfig(deviceUnitConfig.build());
                } catch (CouldNotPerformException ex) {
                    updateExceptionStack = MultiException.push(this, ex, updateExceptionStack);
                }
            }

            MultiException.ExceptionStack finalExceptionStack = null;
            int counter = 0;
            if (newExceptionStack != null) {
                counter = newExceptionStack.size();
            }
            try {
                MultiException.checkAndThrow("Could not register " + counter + " entries", newExceptionStack);
            } catch (CouldNotPerformException ex) {
                finalExceptionStack = MultiException.push(this, ex, finalExceptionStack);
            }

            counter = 0;
            if (updateExceptionStack != null) {
                counter = updateExceptionStack.size();
            }
            try {
                MultiException.checkAndThrow("Could not update " + counter + " entries", updateExceptionStack);
            } catch (CouldNotPerformException ex) {
                finalExceptionStack = MultiException.push(this, ex, finalExceptionStack);
            }
            MultiException.checkAndThrow("Could not synchronize all entries", finalExceptionStack);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not synchronize openHAB things to BCO devices", ex), logger);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

    }

    private void registerDeviceForThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        try {
            DeviceClass deviceClass = getDeviceClassByThing(thingDTO);

            UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
            unitConfig.setType(UnitType.DEVICE);
            DeviceConfig.Builder deviceConfig = unitConfig.getDeviceConfigBuilder();
            deviceConfig.setDeviceClassId(deviceClass.getId());
            deviceConfig.getInventoryStateBuilder().setValue(State.INSTALLED).setTimestamp(TimestampProcessor.getCurrentTimestamp());
            if (thingDTO.location != null) {
                String locationId = getLocationForThing(thingDTO).getId();

                //TODO: add locations flat under root location if not available
                deviceConfig.getInventoryStateBuilder().setLocationId(locationId);
                unitConfig.getPlacementConfigBuilder().setLocationId(locationId);
            }

            // add thing uid to meta config to have a mapping between thing and device
            unitConfig.getMetaConfigBuilder().addEntry(Entry.newBuilder().setKey(THING_UID_KEY).setValue(thingDTO.UID));

            unitConfig.setLabel(thingDTO.label);
            try {
                Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get();
            } catch (ExecutionException ex) {
                throw new CouldNotPerformException("Could not register device for thing[" + thingDTO.thingTypeUID + "]", ex);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private DeviceClass getDeviceClassByThing(final ThingDTO thingDTO) throws CouldNotPerformException {
        final String[] thingTypeUIDSplit = thingDTO.thingTypeUID.split(":");
        if (thingTypeUIDSplit.length != 2) {
            throw new CouldNotPerformException("Could not parse thingTypeUID[" + thingDTO.thingTypeUID + "]. Does not match known pattern binding:deviceInfo");
        }

        // String binding = split[0];
        final String deviceInfo = thingTypeUIDSplit[1];
        final String[] deviceInfoSplit = deviceInfo.split("_");
        if (deviceInfoSplit.length < 2) {
            throw new CouldNotPerformException("Could not parse deviceInfo[" + deviceInfo + "]. Does not match known pattern company_productNumber_...");
        }

        final String company = deviceInfoSplit[0];
        final String productNumber = deviceInfoSplit[1];

        try {
            for (final DeviceClass deviceClass : Registries.getDeviceRegistry().getDeviceClasses()) {
                if (deviceClass.getCompany().equalsIgnoreCase(company)) {
                    //TODO: this mapping has to be done in the meta config of the device class, this could be a fallback
                    if (deviceClass.getProductNumber().replace("-", "").equalsIgnoreCase(productNumber)) {
                        return deviceClass;
                    }
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Interrupted", ex);
        }
        throw new NotAvailableException("Device from company[" + company + "] with productNumber[" + productNumber + "]");
    }

    /**
     * @param thingDTO
     * @return
     * @throws NotAvailableException
     * @throws InterruptedException  TODO: remove interrupted when removed from registry
     */
    private UnitConfig getDeviceForThing(final ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        // iterate over all devices
        for (final UnitConfig deviceUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.DEVICE)) {
            // get the meta config directly in the unit config
            final MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider("UnitMetaConfig", deviceUnitConfig.getMetaConfig()));

            try {
                // get the value for the thing uid key
                String thingUID = metaConfigPool.getValue(THING_UID_KEY);
                // if it matches with the uid of the thing return the device
                if (thingUID.equals(thingDTO.UID)) {
                    return deviceUnitConfig;
                }
            } catch (NotAvailableException ex) {
                // value for device not available so continue
            }
        }

        throw new NotAvailableException("Device for thing[" + thingDTO.UID + "]");
    }

    private UnitConfig getLocationForThing(final ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        if (thingDTO.location != null) {
            List<UnitConfig> locationConfigs = Registries.getLocationRegistry().getLocationConfigsByLabel(thingDTO.location);

            if (locationConfigs.size() == 0) {
                throw new NotAvailableException("Location[" + thingDTO.location + "] for thing[" + thingDTO + "]");
            }

            return locationConfigs.get(0);
        }
        throw new NotAvailableException("Location of thing[" + thingDTO + "]");
    }
}
