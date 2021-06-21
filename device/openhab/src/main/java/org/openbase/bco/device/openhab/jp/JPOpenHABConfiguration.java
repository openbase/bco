package org.openbase.bco.device.openhab.jp;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.AbstractJPDirectory;
import org.openbase.jps.tools.FileHandler;

import java.io.File;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPOpenHABConfiguration extends AbstractJPDirectory {

    public static final String[] COMMAND_IDENTIFIERS = {"--openhab-config"};
    public static final String SYSTEM_VARIABLE_OPENHAB_CONF = "OPENHAB_CONF";
    public static final String DEFAULT_PATH = "/etc/openhab2";

    public JPOpenHABConfiguration() {
        super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.Off);
    }

    @Override
    protected File getPropertyDefaultValue() throws JPNotAvailableException {

        // use system variable if defined
        String systemDefinedPath = System.getenv(SYSTEM_VARIABLE_OPENHAB_CONF);
        if (systemDefinedPath != null) {
            return new File(systemDefinedPath);
        }

        return new File(DEFAULT_PATH);
    }

    @Override
    public String getDescription() {
        return "Defines the openhab configuration directory. This property is based on the system variable " + SYSTEM_VARIABLE_OPENHAB_CONF;
    }

}
