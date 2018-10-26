package org.openbase.bco.dal.remote.printer.jp;

import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.AbstractJPDirectory;
import org.openbase.jps.preset.JPTmpDirectory;
import org.openbase.jps.tools.FileHandler;
import org.openbase.jps.tools.FileHandler.ExistenceHandling;

import java.io.File;

public class JPOutputDirectory extends AbstractJPDirectory {


    public static final String[] COMMAND_IDENTIFIERS = {"--out",};
    public static final FileHandler.ExistenceHandling EXISTENCE_HANDLING = ExistenceHandling.CanExist;
    public static final FileHandler.AutoMode AUTO_MODE = FileHandler.AutoMode.Off;

    public JPOutputDirectory() {
        super(COMMAND_IDENTIFIERS, EXISTENCE_HANDLING, AUTO_MODE);
    }

    @Override
    public File getParentDirectory() throws JPServiceException {
        return JPService.getProperty(JPTmpDirectory.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File("log");
    }

    @Override
    public String getDescription() {
        return "Specifies a file to write the logging to instead printing it to the standard output channel.";
    }

}
