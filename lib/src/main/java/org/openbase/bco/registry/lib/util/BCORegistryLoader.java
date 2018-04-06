package org.openbase.bco.registry.lib.util;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.apache.commons.io.FileUtils;
import org.openbase.bco.authentication.lib.jp.JPBCOShareDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class BCORegistryLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BCORegistryLoader.class);

    /**
     * Method prepares the bco registry by making sure those exist.
     * In case a fresh bco installation has been started, this method creates a new database provided by the bco template folder.
     *
     * @param registryDirectory This file should point to the registry directory to prepare.
     *
     * @throws CouldNotPerformException is thrown if something went wrong during the validation or creation phase.
     */
    public static synchronized void prepareRegistry(final File registryDirectory) throws CouldNotPerformException {
        // regenerate if needed
        if (!registryDirectory.exists()) {
            throw new CouldNotPerformException("RegistryDirectory does not exists so nothing to prepare!");
        }

        // generate new registry if empty
        if (registryDirectory.listFiles().length == 0) {
            generateNewDatabase(registryDirectory);
        }
    }

    /**
     * Method generates a new bco database out the default one provided by the bco template folder.
     *
     * @param databaseFile the file destination to store the new database
     *
     * @throws CouldNotPerformException is thown in case the new database could not be successfully generated.
     */
    private static void generateNewDatabase(final File databaseFile) throws CouldNotPerformException {
        try {
            final File dbTemplateDirectory = new File(JPService.getProperty(JPBCOShareDirectory.class).getValue(), "template");

            if (!dbTemplateDirectory.exists()) {
                throw new InvalidStateException("Database template directory " + dbTemplateDirectory.getAbsolutePath() + " does not exist!");
            }

            final File dbTemplate = new File(dbTemplateDirectory, "db");

            try {
                LOGGER.info("Create new registry db at " + databaseFile.getAbsolutePath() + " based on " + dbTemplate.getAbsolutePath() + ".");
                FileUtils.copyDirectory(dbTemplate, databaseFile);
            } catch (IOException ex) {
                throw new CouldNotPerformException("Could not copy database from template!", ex);
            }
        } catch (final CouldNotPerformException | JPServiceException ex) {
            throw new CouldNotPerformException("Could not generate new database!", ex);
        }
    }
}
