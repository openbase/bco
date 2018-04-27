package org.openbase.bco.app.openhab.manager;

import org.openbase.bco.registry.lib.BCO;
import org.openbase.jul.pattern.launch.AbstractLauncher;

public class OpenHABDeviceManagerLauncher extends AbstractLauncher<OpenHABDeviceManager> {

    public OpenHABDeviceManagerLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(OpenHABDeviceManagerLauncher.class, OpenHABDeviceManager.class);
    }

    @Override
    protected void loadProperties() {
        //TODO: add according properties
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        AbstractLauncher.main(args, OpenHABDeviceManagerLauncher.class, OpenHABDeviceManagerLauncher.class);
    }
}
