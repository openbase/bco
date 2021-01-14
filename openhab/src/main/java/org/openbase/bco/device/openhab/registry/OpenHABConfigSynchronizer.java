package org.openbase.bco.device.openhab.registry;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.device.openhab.registry.synchronizer.*;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenHABConfigSynchronizer implements Launchable<Void>, VoidInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABConfigSynchronizer.class);

    private final SyncObject synchronizationLock = new SyncObject("SyncLock");

    private final InboxApprover inboxApprover;

    private final ThingDeviceUnitSynchronization thingDeviceUnitSynchronization;
    private final DeviceUnitThingSynchronization deviceUnitThingSynchronization;

    private final ThingUnitSynchronization thingUnitSynchronization;
    private final UnitThingSynchronization unitThingSynchronization;

    private final ItemUnitSynchronization itemUnitSynchronization;

    public OpenHABConfigSynchronizer() throws InstantiationException {
        try {
            this.inboxApprover = new InboxApprover();

            this.thingDeviceUnitSynchronization = new ThingDeviceUnitSynchronization(synchronizationLock);
            this.deviceUnitThingSynchronization = new DeviceUnitThingSynchronization(synchronizationLock);

            this.thingUnitSynchronization = new ThingUnitSynchronization(synchronizationLock);
            this.unitThingSynchronization = new UnitThingSynchronization(synchronizationLock);

            this.itemUnitSynchronization = new ItemUnitSynchronization(synchronizationLock);
        } catch (NotAvailableException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() {
    }

    public void activate() throws CouldNotPerformException, InterruptedException {

        SessionManager.getInstance().loginUser(Registries.getUnitRegistry(true).getUnitConfigByAlias(UnitRegistry.OPENHAB_USER_ALIAS).getId(), true);

        thingDeviceUnitSynchronization.activate();
        deviceUnitThingSynchronization.activate();

        thingUnitSynchronization.activate();
        unitThingSynchronization.activate();

        itemUnitSynchronization.activate();

        inboxApprover.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        inboxApprover.deactivate();

        thingDeviceUnitSynchronization.deactivate();
        deviceUnitThingSynchronization.deactivate();

        thingUnitSynchronization.deactivate();
        unitThingSynchronization.deactivate();

        itemUnitSynchronization.deactivate();
    }

    @Override
    public boolean isActive() {
        return thingDeviceUnitSynchronization.isActive()
                && deviceUnitThingSynchronization.isActive()
                && itemUnitSynchronization.isActive()
                && inboxApprover.isActive()
                && thingUnitSynchronization.isActive()
                && unitThingSynchronization.isActive();
    }
}
