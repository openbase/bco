/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.scene.lib.jp;

import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jul.storage.registry.jp.AbstractJPDatabaseDirectory;
import org.dc.jul.storage.registry.jp.JPDatabaseDirectory;
import org.dc.jul.storage.registry.jp.JPInitializeDB;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPSceneClassDatabaseDirectory extends AbstractJPDatabaseDirectory {

    public final static String[] COMMAND_IDENTIFIERS = {"--scene-class-db"};

    public JPSceneClassDatabaseDirectory() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public File getParentDirectory() throws JPNotAvailableException {
        return JPService.getProperty(JPDatabaseDirectory.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File("scene-class-db");
    }

    @Override
    public String getDescription() {
        return "Specifies the scene class database directory. Use  " + JPInitializeDB.COMMAND_IDENTIFIERS[0] + " to auto create database directories.";
    }
}
