/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.jp;

import de.citec.jps.core.JPService;
import de.citec.jps.preset.AbstractJPDirectory;
import de.citec.jps.tools.FileHandler;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPDeviceConfigDatabaseDirectory extends AbstractJPDirectory {

	public final static String[] COMMAND_IDENTIFIERS = {"--device-class-db-dir"};
	
	public JPDeviceConfigDatabaseDirectory() {
		super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.Off);
	}

	@Override
	protected File getPropertyDefaultValue() {
		return new File(JPService.getAttribute(JPDatabaseDirectory.class).getValue(), "device-class-db");
	}

	@Override
	public String getDescription() {
		return "Specifies the device config database location.";
	}
}