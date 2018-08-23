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

import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitThingSynchronization extends AbstractSynchronizer<String, IdentifiableMessage<String, UnitConfig, Builder>> {

    /**
     * Create a new synchronization manager from BCO units to openHAB things.
     *
     * @param synchronizationLock a lock used during a synchronization. This is lock should be shared with
     *                            other synchronization managers so that they will not interfere with each other.
     * @throws InstantiationException if instantiation fails
     * @throws NotAvailableException  if the unit registry is not available
     */
    public UnitThingSynchronization(final SyncObject synchronizationLock) throws InstantiationException, NotAvailableException {
        super(Registries.getUnitRegistry().getUnitConfigRemoteRegistry(), synchronizationLock);
    }

    /**
     * Update the label and location of a thing belonging to the unit.
     *
     * @param identifiableMessage the unit updated
     * @throws CouldNotPerformException if the thing could not be updated
     */
    @Override
    public void update(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        // update thing label and location if needed
        final UnitConfig unitConfig = identifiableMessage.getMessage();
        final EnrichedThingDTO thing = SynchronizationProcessor.getThingForUnit(unitConfig);

        if (SynchronizationProcessor.updateThingToUnit(unitConfig, thing)) {
            OpenHABRestCommunicator.getInstance().updateThing(thing);
        }
    }

    /**
     * Only perform an initial sync between a unit and its thing by calling {@link #update(IdentifiableMessage)}
     * if its the initial sync.
     * Things for units are created through the discovery of the BCO binding.
     *
     * @param identifiableMessage the unit initially registered
     * @throws CouldNotPerformException if the initial update could not be performed.
     */
    @Override
    public void register(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        // if this is the initial sync make sure that things and units are synced
        if (isInitialSync()) {
            update(identifiableMessage);
        }
    }

    /**
     * Remove the thing belonging to a unit.
     *
     * @param identifiableMessage the removed unit
     * @throws CouldNotPerformException if the thing could not be removed or updated
     */
    @Override
    public void remove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        final UnitConfig unitConfig = identifiableMessage.getMessage();

        // retrieve thing
        EnrichedThingDTO thing;
        try {
            thing = SynchronizationProcessor.getThingForUnit(unitConfig);
        } catch (NotAvailableException ex) {
            // do nothing because thing is already removed or never existed
            return;
        }

        // delete thing
        OpenHABRestCommunicator.getInstance().deleteThing(thing);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list of units managed by the unit registry
     * @throws CouldNotPerformException it the units are not available
     */
    @Override
    public List<IdentifiableMessage<String, UnitConfig, Builder>> getEntries() throws CouldNotPerformException {
        return Registries.getUnitRegistry().getUnitConfigRemoteRegistry().getEntries();
    }

    /**
     * Verify that the unit is not a device of object.
     *
     * @param identifiableMessage the unit checked
     * @return if the device managed by openHAB
     */
    @Override
    public boolean verifyEntry(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        // do not manage devices and objects
        //TODO: currently only locations are supported
        switch (identifiableMessage.getMessage().getUnitType()) {
            case LOCATION:
                return true;
            default:
                return false;
        }
    }
}
