/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.jp;

import de.citec.jps.core.JPService;
import de.citec.jps.exception.JPValidationException;
import de.citec.jps.preset.AbstractJPDirectory;
import de.citec.jps.tools.FileHandler;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPLocationConfigDatabaseDirectory extends AbstractJPDirectory {

	public final static String[] COMMAND_IDENTIFIERS = {"--location-config-db"};
	
	public JPLocationConfigDatabaseDirectory() {
		super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.On);
	}
    
    @Override
    public void validate() throws JPValidationException {
        if(JPService.getProperty(JPInitializeDB.class).getValue()) {
            setAutoCreateMode(FileHandler.AutoMode.On);
            setExistenceHandling(FileHandler.ExistenceHandling.MustBeNew);
        }
        super.validate();
    }

    @Override
    public File getParentDirectory() {
        return JPService.getProperty(JPLocationDatabaseDirectory.class).getValue();
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