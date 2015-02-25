/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jp;

import de.citec.jps.preset.AbstractJPDirectory;
import de.citec.jps.tools.FileHandler;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPDeviceManagerConfigPath extends AbstractJPDirectory {

    public final static String ENV_VARIABLE_PREFIX = "prefix";
    public final static String DEFAULT_VOL_LOCATION = "etc/device-data";

    public final static String[] COMMAND_IDENTIFIERS = {"--device-data", "-d"};
    public final static String[] ARGUMENT_IDENTIFIERS = {"DIR"};

    public static FileHandler.ExistenceHandling existenceHandling = FileHandler.ExistenceHandling.Must;
    public static FileHandler.AutoMode autoMode = FileHandler.AutoMode.Off;

    public JPDeviceManagerConfigPath() {
        super(COMMAND_IDENTIFIERS, ARGUMENT_IDENTIFIERS, existenceHandling, autoMode);
    }

    @Override
    protected File getPropertyDefaultValue() {
        File defaultLocation;
        try {
            defaultLocation = new File(System.getenv(ENV_VARIABLE_PREFIX) + File.separator + DEFAULT_VOL_LOCATION);
            if (defaultLocation.exists()) {
                logger.info("Found $" + ENV_VARIABLE_PREFIX + " and use it as config path.");
                return defaultLocation;

            }
        } catch (Exception ex) {
            logger.debug("Could not find $" + ENV_VARIABLE_PREFIX, ex);
        }

        return new File("/tmp/csra/device-data");
    }

    @Override
    public void validate() throws Exception {
        super.validate();
        if (getValueType().equals((ValueType.PropertyDefault))) {
            logger.warn("The default path is used: ["+getValue().getAbsolutePath()+"]. To change this use the "+COMMAND_IDENTIFIERS[0]+" property!");
        }
    }

    @Override
    public String getDescription() {
        return "Setups the device data source directory.";
    }
}
