package org.openbase.bco.app.openhab.jp;

import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.AbstractJPDirectory;
import org.openbase.jps.tools.FileHandler;
import java.io.File;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPOpenHABConfiguration extends AbstractJPDirectory {
    private static final String[] COMMAND_IDENTIFIERS = {"--openhab-config"};

    public JPOpenHABConfiguration() {
        super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.Off);
    }

    @Override
    protected File getPropertyDefaultValue() throws JPNotAvailableException {
        return new File(JPService.getProperty(JPOpenHABDistribution.class).getValue(), "configurations");
    }

    @Override
    public String getDescription() {
        return "Defines the openhab configuration directory.";
    }

}
