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

import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.bco.device.openhab.registry.diff.IdentifiableEnrichedThingDTO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.type.domotic.state.InventoryStateType.InventoryState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Synchronization for things not managed by the bco binding.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ThingDeviceUnitSynchronization extends AbstractSynchronizer<String, IdentifiableEnrichedThingDTO> {

    public ThingDeviceUnitSynchronization(final SyncObject synchronizationLock) throws InstantiationException {
        super(new ThingObservable(), synchronizationLock);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        OpenHABRestCommunicator.getInstance().waitForConnectionState(ConnectionState.State.CONNECTED);
        super.activate();
    }

    @Override
    protected void afterInternalSync() {
        logger.debug("Internal sync finished!");
    }

    @Override
    public void update(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        logger.info("Update {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        final EnrichedThingDTO updatedThing = identifiableEnrichedThingDTO.getDTO();

        // get device unit config for the thing
        try {
            final UnitConfig.Builder deviceUnitConfig = SynchronizationProcessor.getDeviceForThing(updatedThing).toBuilder();

            if (SynchronizationProcessor.updateUnitToThing(updatedThing, deviceUnitConfig)) {
                Registries.getUnitRegistry().updateUnitConfig(deviceUnitConfig.build()).get(30, TimeUnit.SECONDS);
            }
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Unit for thing " + identifiableEnrichedThingDTO.getDTO().UID + " not available", ex, logger, LogLevel.WARN);
        } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
            ExceptionPrinter.printHistory("Unit for thing " + identifiableEnrichedThingDTO.getDTO().UID + " can not be updated!", ex, logger, LogLevel.WARN);
        }
    }

    @Override
    public void register(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        logger.debug("Synchronize {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        final ThingDTO thingDTO = identifiableEnrichedThingDTO.getDTO();

        try {
            final UnitConfig deviceUnitConfig = SynchronizationProcessor.getDeviceForThing(thingDTO);
            // create items for dal units of the device
            for (String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
                SynchronizationProcessor.registerAndValidateItems(Registries.getUnitRegistry().getUnitConfigById(unitId), thingDTO);
            }
        } catch (NotAvailableException ex) {
            // go on to register a device for thing
            registerDevice(thingDTO);
        }
    }

    private void registerDevice(ThingDTO thingDTO) throws CouldNotPerformException, InterruptedException {
        //TODO: should this entire action be rolled back if one part fails?
        // get device class for thing
        DeviceClass deviceClass;
        try {
            deviceClass = SynchronizationProcessor.getDeviceClassForThing(thingDTO);
        } catch (NotSupportedException ex) {
            logger.debug("Ignore not supported thing {} because: {}", thingDTO.UID, ex.getMessage());
            return;
        } catch (NotAvailableException ex) {
            GatewayClass gatewayClass;
            try {
                gatewayClass = SynchronizationProcessor.getGatewayClassForThing(thingDTO);
                logger.warn("Found new Gateway[{}] with Specs[{}] but registration routine not implemented yet :(", LabelProcessor.getBestMatch(gatewayClass.getLabel(), "?"), thingDTO.UID, ex.getMessage());
                return;
            } catch (NotSupportedException exx) {
                logger.debug("Ignore not supported thing {} because: {}", thingDTO.UID, ex.getMessage());
                return;
            } catch (NotAvailableException exx) {
                logger.warn("Ignore thing {} because: {}", thingDTO.UID, ex.getMessage());
                return;
            }
        }

        // create device for this class
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder();
        unitConfig.setUnitType(UnitType.DEVICE);
        unitConfig.getDeviceConfigBuilder().setDeviceClassId(deviceClass.getId());
        unitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setValue(State.INSTALLED).setTimestamp(TimestampProcessor.getCurrentTimestamp());

        String locationId = Registries.getUnitRegistry().getRootLocationConfig().getId();
        // update location according to thing
        if (thingDTO.location != null) {
            locationId = SynchronizationProcessor.getLocationForThing(thingDTO).getId();

            unitConfig.getDeviceConfigBuilder().getInventoryStateBuilder().setLocationId(locationId);
            unitConfig.getPlacementConfigBuilder().setLocationId(locationId);
        } else {
            logger.info("Thing has no location defined so it will be added at the root location");
        }

        String labelSuffix = "";
        int iterration = 2;
        validateLabelLoop:
        while (true) {
            // update label according to thing
            //TODO: load language via bco default config if implemented.

            String unitLabel = thingDTO.label + labelSuffix;

            LabelProcessor.addLabel(unitConfig.getLabelBuilder().clear(), Locale.getDefault(), unitLabel);

            // List need to be created manually since the "filtered" flag is not accessible but its important to even check disabled units
            List<UnitConfig> deviceConfigs = new ArrayList<>();
            for (UnitConfig config : Registries.getUnitRegistry().getUnitConfigsFiltered(false)) {

                // filter non devices
                if (config.getUnitType() != UnitType.DEVICE) {
                    continue;
                }

                // filter devices at another location
                if (!config.getPlacementConfig().getLocationId().equals(locationId)) {
                    continue;
                }

                // filter devices with another label
                if (!LabelProcessor.contains(config.getLabel(), unitLabel)) {
                    continue;
                }

                // continue with new label in case of a name collision
                if (!config.getDeviceConfig().getDeviceClassId().equalsIgnoreCase(deviceClass.getId())) {
                    // device with same label exists but has a different device class, so try to register without suffix
                    labelSuffix = " " + iterration++;
                    continue validateLabelLoop;
                }

                deviceConfigs.add(config);
            }

            // check if label is already taken
            for (UnitConfig config : deviceConfigs) {

                // device with same location, label and class exists so check if it is already connected to a thing
                final MetaConfigVariableProvider metaConfigVariableProvider = new MetaConfigVariableProvider(config.getAlias(0) + "MetaConfig", config.getMetaConfig());
                try {
                    final String thingUID = metaConfigVariableProvider.getValue(SynchronizationProcessor.OPENHAB_THING_UID_KEY);

                    if (thingDTO.UID.equals(thingUID)) {
                        logger.warn("skip registration because thing {} is already registered as device {}", thingDTO.UID, LabelProcessor.getBestMatch(config.getLabel(), "?"));
                        return;
                    }
                    // same class, label and location but meta config entry differs so the collision has to be resolved by setting a new label.
                    labelSuffix = " " + iterration++;
                    continue validateLabelLoop;

                } catch (NotAvailableException ex) {
                    // thing matches to device but the meta config entry is missing so add it and exit method.
                    final Builder builder = config.toBuilder();
                    builder.getMetaConfigBuilder().addEntryBuilder().setKey(SynchronizationProcessor.OPENHAB_THING_UID_KEY).setValue(thingDTO.UID);
                    try {
                        Registries.getUnitRegistry().updateUnitConfig(builder.build()).get();
                        return;
                    } catch (ExecutionException e) {
                        throw new CouldNotPerformException("Could not update OPENHAB_THING_UID_KEY in device " + config.getAlias(0));
                    }
                }
            }
            // no collision found, so continue...
            break validateLabelLoop;
        }

        // add thing uid to meta config to have a mapping between thing and device
        unitConfig.getMetaConfigBuilder().addEntryBuilder().setKey(SynchronizationProcessor.OPENHAB_THING_UID_KEY).setValue(thingDTO.UID);
        // if available also set the unique id which allows to bypass things if multiple things need to be mapped to a single device unit
        if (thingDTO.properties.containsKey(SynchronizationProcessor.OPENHAB_THING_PROPERTY_KEY_UNIQUE_ID)) {
            final String uniqueId = thingDTO.properties.get(SynchronizationProcessor.OPENHAB_THING_PROPERTY_KEY_UNIQUE_ID);
            unitConfig.getMetaConfigBuilder().addEntryBuilder().setKey(SynchronizationProcessor.OPENHAB_UNIQUE_ID_KEY).setValue(SynchronizationProcessor.getUniquePrefix(uniqueId));
        }

        try {
            final UnitConfig deviceUnitConfig = Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get();

            // create items for dal units of the device
            for (String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
                SynchronizationProcessor.registerAndValidateItems(Registries.getUnitRegistry().getUnitConfigById(unitId), thingDTO);
            }
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register device for thing[" + thingDTO.thingTypeUID + "]", ex);
        }
    }

    @Override
    public void remove(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        logger.info("Remove {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        final EnrichedThingDTO enrichedThingDTO = identifiableEnrichedThingDTO.getDTO();

        // get device unit config for thing
        UnitConfig deviceUnitConfig;
        try {
            deviceUnitConfig = SynchronizationProcessor.getDeviceForThing(enrichedThingDTO);
        } catch (NotAvailableException ex) {
            // do nothing if no device exists
            return;
        }

        // remove device
        try {
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
    public boolean isSupported(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) {
        // only handle things not managed by the bco binding
        return !identifiableEnrichedThingDTO.getId().startsWith(ThingUnitSynchronization.BCO_BINDING_ID);
    }
}
