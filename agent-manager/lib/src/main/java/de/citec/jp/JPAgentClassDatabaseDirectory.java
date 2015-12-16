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
import org.dc.jps.tools.FileHandler;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.storage.registry.jp.AbstractJPDatabaseDirectory;
import de.citec.jul.storage.registry.jp.JPDatabaseDirectory;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPAgentClassDatabaseDirectory extends AbstractJPDatabaseDirectory {

    public final static String[] COMMAND_IDENTIFIERS = {"--agent-class-db"};

    public JPAgentClassDatabaseDirectory() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public File getParentDirectory() throws JPNotAvailableException {
        return JPService.getProperty(JPDatabaseDirectory.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File("agent-class-db");
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
        super.validate();
    }

    @Override
    public String getDescription() {
        return "Specifies the agent class database directory. Use  " + JPInitializeDB.COMMAND_IDENTIFIERS[0] + " to auto create database directories.";
    }
}
