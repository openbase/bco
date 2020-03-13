package org.openbase.bco.app.util.launch.jp;

import org.openbase.bco.app.util.launch.jp.JPDeviceManager.BuildinDeviceManager;
import org.openbase.bco.device.openhab.OpenHABDeviceManagerLauncher;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.AbstractJPEnum;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.jul.processing.StringProcessor;

import java.util.Arrays;

public class JPDeviceManager extends AbstractJPEnum<BuildinDeviceManager> {

    public final static String[] COMMAND_IDENTIFIERS = {"--device-manager"};

    public enum BuildinDeviceManager {

        NON(null),
        OPENHAB(OpenHABDeviceManagerLauncher.class);

        public final Class<? extends AbstractLauncher<?>> launcher;

        BuildinDeviceManager(final Class<? extends AbstractLauncher<?>> launcher) {
            this.launcher = launcher;
        }
    }

    @Override
    protected BuildinDeviceManager getPropertyDefaultValue() throws JPNotAvailableException {

        return BuildinDeviceManager.NON;
    }

    public JPDeviceManager() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public String getDescription() {
        return "Can be used to declare a list of device mangers (e.g.: "+ StringProcessor.transformCollectionToString(Arrays.asList(BuildinDeviceManager.values()), ", ") +") to launch within this bco instance.";
    }
}
