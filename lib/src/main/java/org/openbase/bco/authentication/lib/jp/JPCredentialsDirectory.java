package org.openbase.bco.authentication.lib.jp;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2020 openbase.org
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
import org.openbase.jps.tools.FileHandler;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;

import java.io.File;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPCredentialsDirectory extends AbstractJPDirectory {

    public static final String DEFAULT_CREDENTIALS_PATH = "credentials";
    public static final String[] COMMAND_IDENTIFIERS = {"--cr", "--credentials"};

    public static final FileHandler.ExistenceHandling EXISTENCE_HANDLING = FileHandler.ExistenceHandling.Must;
    public static final FileHandler.AutoMode AUTO_MODE = FileHandler.AutoMode.On;

    public JPCredentialsDirectory() {
        super(COMMAND_IDENTIFIERS, EXISTENCE_HANDLING, AUTO_MODE);
        registerDependingProperty(JPBCOVarDirectory.class);
        registerDependingProperty(JPResetCredentials.class);
    }

    @Override
    public File getParentDirectory() throws JPServiceException {
        if (JPService.getProperty(JPBCOVarDirectory.class).getValue().exists() || JPService.testMode()) {
            return JPService.getProperty(JPBCOVarDirectory.class).getValue();
        }
        throw new JPServiceException("Could not auto detect bco var path!");
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File(DEFAULT_CREDENTIALS_PATH);
    }

    @Override
    public String getDescription() {
        return "Specifies the credential directory. If not already exist, this credential directory is auto created during startup.";
    }

    @Override
    public void validate() throws JPValidationException {
        try {
            if (JPService.getProperty(JPResetCredentials.class).getValue()) {
                setAutoCreateMode(FileHandler.AutoMode.On);
                setExistenceHandling(FileHandler.ExistenceHandling.MustBeNew);
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }
        super.validate();
    }
}
