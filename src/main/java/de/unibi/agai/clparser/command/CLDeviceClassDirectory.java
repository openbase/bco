/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unibi.agai.clparser.command;

import de.unibi.agai.clparser.CLParser;
import de.unibi.agai.tools.FileHandler;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class CLDeviceClassDirectory extends AbstractCLDirectory {

	public final static String[] COMMAND_IDENTIFIERS = {"--deviceClassDirectory"};
	public final static String[] ARGUMENT_IDENTIFIERS = {"DIR"};

	public static FileHandler.ExistenceHandling existenceHandling = FileHandler.ExistenceHandling.Must;
	public static FileHandler.AutoMode autoMode = FileHandler.AutoMode.On;
	
	public CLDeviceClassDirectory() {
		super(COMMAND_IDENTIFIERS, ARGUMENT_IDENTIFIERS, existenceHandling, autoMode);
	}

	@Override
	protected File getCommandDefaultValue() {
		return new File(CLParser.getAttribute(CLDeviceManagerConfigPath.class).getValue(), "device-class");
	}

	@Override
	public String getDescription() {
		return "Specifies the DeviceClass directory name.";
	}
}