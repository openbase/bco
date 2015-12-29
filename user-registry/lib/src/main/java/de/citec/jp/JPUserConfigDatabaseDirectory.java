/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jp;

import org.dc.jul.storage.registry.jp.AbstractJPDatabaseDirectory;
import org.dc.jul.storage.registry.jp.JPDatabaseDirectory;
import org.dc.jul.storage.registry.jp.JPInitializeDB;
import java.io.File;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;

/**
 *
 * @author mpohling
 */
public class JPUserConfigDatabaseDirectory extends AbstractJPDatabaseDirectory {

    public final static String[] COMMAND_IDENTIFIERS = {"--user-config-db"};

    public JPUserConfigDatabaseDirectory() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public File getParentDirectory() throws JPNotAvailableException {
        return JPService.getProperty(JPDatabaseDirectory.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File("user-config-db");
    }

    @Override
    public String getDescription() {
        return "Specifies the user config database directory. Use  " + JPInitializeDB.COMMAND_IDENTIFIERS[0] + " to auto create database directories.";
    }
}
