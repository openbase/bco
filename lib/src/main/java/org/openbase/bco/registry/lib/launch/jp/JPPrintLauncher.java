package org.openbase.bco.registry.lib.launch.jp;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.jps.preset.AbstractJPBoolean;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPPrintLauncher extends AbstractJPBoolean {
	
	public final static String[] COMMAND_IDENTIFIERS = {"--print-launcher"};

	public JPPrintLauncher() {
		super(COMMAND_IDENTIFIERS);
	}
	
	@Override
	public String getDescription() {
		return "Prints a list of all available launcher and exit.";
	}
}
