/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.jp;

import de.citec.jps.core.JPService;
import de.citec.jps.preset.AbstractJPFile;
import de.citec.jps.tools.FileHandler;
import de.citec.jps.tools.FileHandler.AutoMode;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPDatabaseFile extends AbstractJPFile {

	public final static String[] COMMAND_IDENTIFIERS = {"--database-file"};
	
	public JPDatabaseFile() {
		super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, AutoMode.Off);
	}

	@Override
	protected File getPropertyDefaultValue() {
		return new File(JPService.getAttribute(JPDatabaseDirectory.class).getValue(), "db.json");
	}

	@Override
	public String getDescription() {
		return "Specifies the database file.";
	}
}