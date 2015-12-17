/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jp;

import de.citec.jul.storage.registry.jp.AbstractJPDatabaseDirectory;
import de.citec.jul.storage.registry.jp.JPDatabaseDirectory;
import java.io.File;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;

/**
 *
 * @author mpohling
 */
public class JPConnectionConfigDatabaseDirectory extends AbstractJPDatabaseDirectory {

    public final static String[] COMMAND_IDENTIFIERS = {"--connection-config-db"};

    public JPConnectionConfigDatabaseDirectory() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public File getParentDirectory() throws JPNotAvailableException {
        return JPService.getProperty(JPDatabaseDirectory.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File("connection-config-db");
    }

    @Override
    public String getDescription() {
        return "Specifies the connection config database directory.";
    }
}
