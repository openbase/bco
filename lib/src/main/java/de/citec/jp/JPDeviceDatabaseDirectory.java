/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.jp;

import de.citec.jul.storage.jp.JPInitializeDB;
import de.citec.jps.core.JPService;
import de.citec.jps.exception.ValidationException;
import de.citec.jps.preset.AbstractJPDirectory;
import de.citec.jps.tools.FileHandler;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPDeviceDatabaseDirectory extends AbstractJPDirectory {

	public final static String[] COMMAND_IDENTIFIERS = {"--db", "--database"};

	public static FileHandler.ExistenceHandling existenceHandling = FileHandler.ExistenceHandling.Must;
	public static FileHandler.AutoMode autoMode = FileHandler.AutoMode.On;
	
	public JPDeviceDatabaseDirectory() {
		super(COMMAND_IDENTIFIERS, existenceHandling, autoMode);
	}
    
    @Override
    public void validate() throws ValidationException {
        if(JPService.getProperty(JPInitializeDB.class).getValue()) {
            setAutoCreateMode(FileHandler.AutoMode.On);
            setExistenceHandling(FileHandler.ExistenceHandling.MustBeNew);
        }
        super.validate();
    }

	@Override
	protected File getPropertyDefaultValue() {
		return new File(System.getProperty("user.home")+"/.local/share/device-manager/db");
	}

	@Override
	public String getDescription() {
		return "Specifies the device database directory.";
	}
}