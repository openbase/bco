package org.openbase.bco.app.openhab;

import org.openbase.bco.app.openhab.manager.OpenHABDeviceManagerLauncher;
import org.openbase.bco.app.openhab.registry.OpenHABConfigSynchronizerLauncher;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jul.pattern.launch.AbstractLauncher;

public class OpenHABApp {

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        AbstractLauncher.main(args, OpenHABApp.class,
                OpenHABConfigSynchronizerLauncher.class,
                OpenHABDeviceManagerLauncher.class
        );
    }
}
