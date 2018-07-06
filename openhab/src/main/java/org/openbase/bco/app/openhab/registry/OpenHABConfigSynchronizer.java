package org.openbase.bco.app.openhab.registry;

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
import com.google.gson.JsonObject;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.registry.synchronizer.InboxApprover;
import org.openbase.bco.app.openhab.registry.synchronizer.ThingDeviceUnitSynchronizer;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenHABConfigSynchronizer implements Launchable<Void>, VoidInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenHABConfigSynchronizer.class);

    private final ThingDeviceUnitSynchronizer thingDeviceUnitSynchronizer;
    private final InboxApprover inboxApprover;

    public OpenHABConfigSynchronizer() throws InstantiationException {
        this.thingDeviceUnitSynchronizer = new ThingDeviceUnitSynchronizer();
        this.inboxApprover = new InboxApprover();
    }

    public void init() {
    }

    public void activate() throws CouldNotPerformException, InterruptedException {
        Registries.waitForData();
        thingDeviceUnitSynchronizer.activate();
        inboxApprover.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        inboxApprover.deactivate();
        thingDeviceUnitSynchronizer.deactivate();
    }

    @Override
    public boolean isActive() {
        return thingDeviceUnitSynchronizer.isActive() && inboxApprover.isActive();
    }
}
