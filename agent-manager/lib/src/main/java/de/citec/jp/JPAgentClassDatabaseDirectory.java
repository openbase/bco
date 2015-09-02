/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.jp;

import de.citec.jul.storage.registry.jp.JPInitializeDB;
import de.citec.jps.core.JPService;
import de.citec.jps.exception.JPValidationException;
import de.citec.jps.preset.AbstractJPDirectory;
import de.citec.jps.tools.FileHandler;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPAgentClassDatabaseDirectory extends AbstractJPDirectory {

	public final static String[] COMMAND_IDENTIFIERS = {"--agent-class-db"};
	
	public JPAgentClassDatabaseDirectory() {
		super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.Off);
	}

    @Override
    public File getParentDirectory() {
        return JPService.getProperty(JPAgentDatabaseDirectory.class).getValue();
    }
    
	@Override
	protected File getPropertyDefaultValue() {
		return new File("agent-class-db");
    }

    @Override
    public void validate() throws JPValidationException {
        if(JPService.getProperty(JPInitializeDB.class).getValue()) {
            setAutoCreateMode(FileHandler.AutoMode.On);
            setExistenceHandling(FileHandler.ExistenceHandling.Must);
        }
        super.validate();
    }

	@Override
	public String getDescription() {
		return "Specifies the agent class database directory. Use  "+JPInitializeDB.COMMAND_IDENTIFIERS[0]+ " to auto create database directories.";
	}
}