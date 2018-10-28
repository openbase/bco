package org.openbase.bco.dal.remote.printer.jp;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.AbstractJPDirectory;
import org.openbase.jps.preset.JPTmpDirectory;
import org.openbase.jps.tools.FileHandler;
import org.openbase.jps.tools.FileHandler.ExistenceHandling;

import java.io.File;

public class JPOutputDirectory extends AbstractJPDirectory {


    public static final String[] COMMAND_IDENTIFIERS = {"--out",};
    public static final FileHandler.ExistenceHandling EXISTENCE_HANDLING = ExistenceHandling.CanExist;
    public static final FileHandler.AutoMode AUTO_MODE = FileHandler.AutoMode.Off;

    public JPOutputDirectory() {
        super(COMMAND_IDENTIFIERS, EXISTENCE_HANDLING, AUTO_MODE);
    }

    @Override
    public File getParentDirectory() throws JPServiceException {
        return JPService.getProperty(JPTmpDirectory.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File("log");
    }

    @Override
    public String getDescription() {
        return "Specifies a file to write the logging to instead printing it to the standard output channel.";
    }

}
