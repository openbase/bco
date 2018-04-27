package org.openbase.bco.app.openhab.registry;

import org.openbase.bco.app.openhab.registry.synchronizer.ThingDeviceUnitSynchronizer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenHABConfigSynchronizer implements Launchable<Void>, VoidInitializable {


    public static String OPENHAB_THING_TYPE_UID_KEY = "OPENHAB_THING_TYPE_UID";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ThingDeviceUnitSynchronizer thingDeviceUnitSynchronizer;


    public OpenHABConfigSynchronizer() throws InstantiationException {
        thingDeviceUnitSynchronizer = new ThingDeviceUnitSynchronizer();
    }


    public void init() {
    }

    public void activate() throws CouldNotPerformException, InterruptedException {
        thingDeviceUnitSynchronizer.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        thingDeviceUnitSynchronizer.deactivate();
    }

    @Override
    public boolean isActive() {
        return thingDeviceUnitSynchronizer.isActive();
    }
}
