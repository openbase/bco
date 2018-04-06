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
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.exception.JPValidationException;
import org.openbase.jps.preset.AbstractJPDirectory;
import org.openbase.jps.preset.JPPrefix;
import org.openbase.jps.tools.FileHandler;

import java.io.File;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPBCOShareDirectory extends AbstractJPDirectory {

    public static final String DEFAULT_PATH = "share/bco";
    public static final String[] COMMAND_IDENTIFIERS = {"--bco-share"};

    public static final FileHandler.ExistenceHandling EXISTENCE_HANDLING = FileHandler.ExistenceHandling.Must;
    public static final FileHandler.AutoMode AUTO_MODE = FileHandler.AutoMode.Off;

    /**
     * Default constructor needed for reflection access.
     */
    public JPBCOShareDirectory() {
        super(COMMAND_IDENTIFIERS, EXISTENCE_HANDLING, AUTO_MODE);
        registerDependingProperty(JPPrefix.class);
    }

    @Override
    public File getParentDirectory() throws JPServiceException {
        // declare default parent
        return JPService.getProperty(JPPrefix.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File(DEFAULT_PATH);
    }

    @Override
    public void validate() throws JPValidationException {
        if (JPService.testMode()) {
            setAutoCreateMode(FileHandler.AutoMode.On);
            setExistenceHandling(FileHandler.ExistenceHandling.Must);
        }
        super.validate();
    }

    @Override
    public String getDescription() {
        return "Property can be used to overwrite the variable data folder of bco. Those is per default placed at the bco home path and hosts the registry db.";
    }
}
