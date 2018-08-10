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

import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceThingSynchronization extends AbstractSynchronizer<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    public DeviceThingSynchronization(final SyncObject synchronizationLock) throws InstantiationException, NotAvailableException {
        super(Registries.getUnitRegistry().getDeviceUnitConfigRemoteRegistry(), synchronizationLock);
    }

    @Override
    public void update(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.info("Update device[" + identifiableMessage.getMessage().getAlias(0) + "]");
        final UnitConfig deviceUnitConfig = identifiableMessage.getMessage();
        final EnrichedThingDTO thing = SynchronizationHelper.getThingForDevice(deviceUnitConfig);

        if (updateThing(deviceUnitConfig, thing)) {
            logger.info("Update thing[" + thing.UID + "]");
            OpenHABRestCommunicator.getInstance().updateThing(thing);
        }
    }

    @Override
    public void register(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.info("Update device[" + identifiableMessage.getMessage().getAlias(0) + "]");
        // if this is the initial sync make sure that things and devices are synced
        if (isInitialSync()) {
            update(identifiableMessage);
        }

        // devices for openHAB will currently only registered via openHAB itself
    }

    @Override
    public void remove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.info("Remove device[" + identifiableMessage.getMessage().getAlias(0) + "]");
        final UnitConfig deviceUnitConfig = identifiableMessage.getMessage();

        EnrichedThingDTO thing;
        try {
            thing = SynchronizationHelper.getThingForDevice(deviceUnitConfig);
        } catch (NotAvailableException ex) {
            // do nothing because thing is already removed
            return;
        }

        logger.info("Test if other device for thing exist");
        // if there is another device corresponding to this thing, do not delete it because the other
        // device has simple taken over
        for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.DEVICE)) {
            if (unitConfig.getId().equals(deviceUnitConfig.getId())) {
                continue;
            }

            try {
                String thingId = SynchronizationHelper.getThingIdFromDevice(unitConfig);
                // perform update for other device
                if (thingId.equals(thing.UID)) {
                    if (updateThing(unitConfig, thing)) {
                        OpenHABRestCommunicator.getInstance().updateThing(thing);
                        for (final String unitId : unitConfig.getDeviceConfig().getUnitIdList()) {
                            SynchronizationHelper.registerAndValidateItems(Registries.getUnitRegistry().getUnitConfigById(unitId), thing);
                        }
                    }
                    return;
                }
            } catch (NotAvailableException ex) {
                // do nothing
            }
        }

        logger.info("Delete thing[" + thing.UID + "]");
        OpenHABRestCommunicator.getInstance().deleteThing(thing);
    }

    @Override
    public List<IdentifiableMessage<String, UnitConfig, Builder>> getEntries() throws CouldNotPerformException {
        return Registries.getUnitRegistry().getDeviceUnitConfigRemoteRegistry().getEntries();
    }

    @Override
    public boolean verifyEntry(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        // validate that the device is configured via openHAB
        try {
            SynchronizationHelper.getThingIdFromDevice(identifiableMessage.getMessage());
            return true;
        } catch (NotAvailableException ex) {
            return false;
        }
    }

    private boolean updateThing(final UnitConfig deviceUnitConfig, final EnrichedThingDTO thing) throws CouldNotPerformException {
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
}
