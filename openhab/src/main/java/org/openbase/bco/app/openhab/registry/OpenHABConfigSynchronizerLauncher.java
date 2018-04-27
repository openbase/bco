package org.openbase.bco.app.openhab.registry;

import org.openbase.bco.app.openhab.manager.OpenHABDeviceManagerLauncher;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.launch.AbstractLauncher;

public class OpenHABConfigSynchronizerLauncher extends AbstractLauncher<OpenHABConfigSynchronizer> {


    public OpenHABConfigSynchronizerLauncher() throws InstantiationException {
        super(OpenHABConfigSynchronizerLauncher.class, OpenHABConfigSynchronizer.class);
    }

    @Override
    protected void loadProperties() {

    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        AbstractLauncher.main(args, OpenHABConfigSynchronizerLauncher.class, OpenHABConfigSynchronizerLauncher.class);
    }
}
