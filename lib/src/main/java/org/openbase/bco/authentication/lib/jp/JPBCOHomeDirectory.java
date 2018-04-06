package org.openbase.bco.authentication.lib.jp;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.AbstractJPDirectory;
import org.openbase.jps.preset.JPLocalUserPrefix;
import org.openbase.jps.preset.JPTmpDirectory;
import org.openbase.jps.tools.FileHandler;
import org.openbase.jul.exception.FatalImplementationErrorException;

import java.io.File;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPBCOHomeDirectory extends AbstractJPDirectory {

    public static final String SYSTEM_VARIABLE_BCO_HOME = "BCO_HOME";
    public static final String DEFAULT_PATH = ".config/bco";
    public static final String[] COMMAND_IDENTIFIERS = {"--bco-home"};

    public static final FileHandler.ExistenceHandling EXISTENCE_HANDLING = FileHandler.ExistenceHandling.Must;
    public static final FileHandler.AutoMode AUTO_MODE = FileHandler.AutoMode.On;

    /**
     * Default constructor needed for reflection access.
     */
    public JPBCOHomeDirectory() {
        super(COMMAND_IDENTIFIERS, EXISTENCE_HANDLING, AUTO_MODE);
        registerDependingProperty(JPTmpDirectory.class);
        registerDependingProperty(JPLocalUserPrefix.class);
    }

    @Override
    public File getParentDirectory() throws JPServiceException {
        // declare default parent
        return JPService.getProperty(JPLocalUserPrefix.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {

        // use tmp folder in test case
        if (JPService.testMode()) {
            try {
                return JPService.getProperty(JPTmpDirectory.class).getValue();
            } catch (JPNotAvailableException ex) {
                // exception is already printed in constructore so no further forwarding needed.
                new FatalImplementationErrorException("Could not link test tmp directory as bco home path for java tests.", this, ex);
            }
        }

        // use bco home system variable if defined
        String systemDefinedHome = System.getenv(SYSTEM_VARIABLE_BCO_HOME);
        if (systemDefinedHome != null) {
            return new File(systemDefinedHome);
        }

        return new File(DEFAULT_PATH);
    }

    @Override
    public String getDescription() {
        return "Property can be used to overwrite the bco home path which is mainly used for storing variable runtime data including the registry db.";
    }
}
