/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jp;

import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import de.citec.jul.storage.registry.jp.AbstractJPDatabaseDirectory;
import de.citec.jul.storage.registry.jp.JPDatabaseDirectory;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPAgentConfigDatabaseDirectory extends AbstractJPDatabaseDirectory {

    public final static String[] COMMAND_IDENTIFIERS = {"--agent-config-db"};

    public JPAgentConfigDatabaseDirectory() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public File getParentDirectory() throws JPNotAvailableException {
        return JPService.getProperty(JPDatabaseDirectory.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File("agent-config-db");
    }

    @Override
    public String getDescription() {
        return "Specifies the agent config database directory. Use  " + JPInitializeDB.COMMAND_IDENTIFIERS[0] + " to auto create database directories.";
    }
}
