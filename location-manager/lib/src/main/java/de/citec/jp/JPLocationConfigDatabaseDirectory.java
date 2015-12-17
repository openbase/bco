/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jp;

import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.exception.JPValidationException;
import org.dc.jps.preset.AbstractJPDirectory;
import org.dc.jps.preset.JPHelp;
import org.dc.jps.tools.FileHandler;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.storage.registry.jp.JPDatabaseDirectory;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import de.citec.jul.storage.registry.jp.JPResetDB;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPLocationConfigDatabaseDirectory extends AbstractJPDirectory {

    public final static String[] COMMAND_IDENTIFIERS = {"--location-config-db"};

    public JPLocationConfigDatabaseDirectory() {
        super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.Off);
    }

    @Override
    public File getParentDirectory() throws JPNotAvailableException {
        return JPService.getProperty(JPDatabaseDirectory.class).getValue();
    }

    @Override
    public void validate() throws JPValidationException {
        try {
            if (JPService.getProperty(JPInitializeDB.class).getValue()) {
                setAutoCreateMode(FileHandler.AutoMode.On);
                setExistenceHandling(FileHandler.ExistenceHandling.Must);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        try {
            if (JPService.getProperty(JPResetDB.class).getValue()) {
                setAutoCreateMode(FileHandler.AutoMode.On);
                setExistenceHandling(FileHandler.ExistenceHandling.MustBeNew);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        super.validate();

        if (!getValue().exists()) {
            throw new JPValidationException("Could not detect database! You can use the argument " + JPInitializeDB.COMMAND_IDENTIFIERS[0] + " to initialize a new db enviroment. Use " + JPHelp.COMMAND_IDENTIFIERS[0] + " to get more options.");
        }
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File("location-config-db");
    }

    @Override
    public String getDescription() {
        return "Specifies the location config database directory.";
    }
}
