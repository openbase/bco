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
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;

/**
 * Class managing the synchronization from BCO device units to openHAB things.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceThingSynchronization extends AbstractSynchronizer<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> {

    /**
     * Create a new synchronization manager from BCO device units to openHAB things.
     *
     * @param synchronizationLock a lock used during a synchronization. This is lock should be shared with
     *                            other synchronization managers so that they will not interfere with each other.
     * @throws InstantiationException if instantiation fails
     * @throws NotAvailableException  if the unit registry is not available
     */
    public DeviceThingSynchronization(final SyncObject synchronizationLock) throws InstantiationException, NotAvailableException {
        super(Registries.getUnitRegistry().getDeviceUnitConfigRemoteRegistry(), synchronizationLock);
    }

    /**
     * Update the label and location of a thing belonging to the device unit.
     *
     * @param identifiableMessage the device unit updated
     * @throws CouldNotPerformException if the thing could not be updated
     */
    @Override
    public void update(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        // update thing label and location if needed
        final UnitConfig deviceUnitConfig = identifiableMessage.getMessage();
        final EnrichedThingDTO thing = SynchronizationProcessor.getThingForDevice(deviceUnitConfig);

        if (SynchronizationProcessor.updateThingToUnit(deviceUnitConfig, thing)) {
            OpenHABRestCommunicator.getInstance().updateThing(thing);
        }
    }

    /**
     * Only perform an initial sync between a device unit and its thing by calling {@link #update(IdentifiableMessage)}
     * if its the initial sync.
     *
     * @param identifiableMessage the device unit initially registered
     * @throws CouldNotPerformException if the initial update could not be performed.
     */
    @Override
    public void register(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        // if this is the initial sync make sure that things and devices are synced
        if (isInitialSync()) {
            update(identifiableMessage);
        }

        // devices for openHAB are only registered via openHAB itself
    }

    /**
     * Remove the thing belonging to a device unit. This is skipped if the thing is not available, e.g. because
     * it has already been removed, or if another device still manages the thing.
     * If another device manages the thing, the thing and its items will be updated. This is necessary for a
     * synchronization with an old BCO registry.
     *
     * @param identifiableMessage the removed device unit
     * @throws CouldNotPerformException if the thing could not be removed or updated
     */
    @Override
    public void remove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
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
        for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.DEVICE)) {
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
        OpenHABRestCommunicator.getInstance().deleteThing(thing);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list of device units managed by the unit registry
     * @throws CouldNotPerformException it the device units are not available
     */
    @Override
    public List<IdentifiableMessage<String, UnitConfig, Builder>> getEntries() throws CouldNotPerformException {
        return Registries.getUnitRegistry().getDeviceUnitConfigRemoteRegistry().getEntries();
    }

    /**
     * Verify that the device is managed by openHAB.
     *
     * @param identifiableMessage the device unit checked
     * @return if the device managed by openHAB
     */
    @Override
    public boolean verifyEntry(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        // validate that the device is configured via openHAB
        try {
            SynchronizationProcessor.getThingIdFromDevice(identifiableMessage.getMessage());
            return true;
        } catch (NotAvailableException ex) {
            return false;
        }
    }
}
