package org.openbase.bco.device.openhab.registry.synchronizer;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2019 openbase.org
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
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.List;

/**
 * Synchronization for units handled by the BCO binding. This synchronization routine merely removes a thing
 * if the according unit is removed because updating the thing configuration on unit config updates is done by
 * the binding itself.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitThingSynchronization extends AbstractSynchronizer<String, IdentifiableMessage<String, UnitConfig, Builder>> {

    /**
     * Create a new synchronization test from BCO units to openHAB things.
     *
     * @param synchronizationLock a lock used during a synchronization. This is lock should be shared with
     *                            other synchronization managers so that they will not interfere with each other.
     *
     * @throws InstantiationException if instantiation fails
     * @throws NotAvailableException  if the unit registry is not available
     */
    public UnitThingSynchronization(final SyncObject synchronizationLock) throws InstantiationException, NotAvailableException {
        super(Registries.getUnitRegistry().getUnitConfigRemoteRegistry(), synchronizationLock);
    }

    @Override
    protected void afterInternalSync() {
        logger.info("Internal sync finished!");
    }

    /**
     * Do nothing.
     *
     * @param identifiableMessage the unit updated
     *
     * @throws CouldNotPerformException if the thing could not be updated
     */
    @Override
    public void update(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.trace("Synchronize update {} ...", identifiableMessage);
        // do nothing because the binding itself updates the according thing configuration
    }

    /**
     * Do nothing.
     *
     * @param identifiableMessage the unit initially registered
     *
     * @throws CouldNotPerformException if the initial update could not be performed.
     */
    @Override
    public void register(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.trace("Synchronize registration {} ...", identifiableMessage);
        // do nothing because the binding itself updates the according thing configuration
    }

    /**
     * Remove the thing belonging to a unit.
     *
     * @param identifiableMessage the removed unit
     *
     * @throws CouldNotPerformException if the thing could not be removed or updated
     */
    @Override
    public void remove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.trace("Synchronize removal {} ...", identifiableMessage);
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
        SynchronizationProcessor.deleteThing(thing);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list of units managed by the unit registry
     *
     * @throws CouldNotPerformException it the units are not available
     */
    @Override
    public List<IdentifiableMessage<String, UnitConfig, Builder>> getEntries() throws CouldNotPerformException {
        return Registries.getUnitRegistry().getUnitConfigRemoteRegistry().getEntries();
    }

    /**
     * Verify that the unit is handled by the binding.
     *
     * @param identifiableMessage the unit checked
     *
     * @return if the device managed by the bco binding
     */
    @Override
    public boolean isSupported(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) {
        try {
            return handledByBCOBinding(identifiableMessage.getMessage());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not verify unit " + identifiableMessage.getMessage().getAlias(0), ex, logger);
            return false;
        }
    }

    private boolean handledByBCOBinding(final UnitConfig unitConfig) throws CouldNotPerformException {
        // ignore all units without services
        if (unitConfig.getServiceConfigCount() == 0) {
            return false;
        }

        // ignore system users
        if (unitConfig.getUnitType() == UnitType.USER && unitConfig.getUserConfig().getSystemUser()) {
            return false;
        }

        // ignore all units from devices handled by the openhab app
        if (UnitConfigProcessor.isHostUnitAvailable(unitConfig)) {
            UnitConfig unitHost = Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId());
            if (unitHost.getUnitType() == UnitType.DEVICE) {
                DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(unitHost.getDeviceConfig().getDeviceClassId());
                return !deviceClass.getBindingConfig().getBindingId().equalsIgnoreCase("openhab");
            }
        }

        // accept everything else
        return true;
    }
}
