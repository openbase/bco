package org.openbase.bco.registry.lib.jp;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.authentication.lib.jp.JPBCOVarDirectory;
import org.openbase.bco.registry.lib.util.BCORegistryLoader;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.exception.JPValidationException;
import org.openbase.jps.preset.AbstractJPDirectory;
import org.openbase.jps.preset.JPShareDirectory;
import org.openbase.jps.preset.JPVarDirectory;
import org.openbase.jps.tools.FileHandler;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPBCODatabaseDirectory extends AbstractJPDirectory {

    public static final String DEFAULT_DB_PATH = "registry/db";
    public static final String[] COMMAND_IDENTIFIERS = {"--db", "--database"};

    public static final FileHandler.ExistenceHandling EXISTENCE_HANDLING = FileHandler.ExistenceHandling.Must;
    public static final FileHandler.AutoMode AUTO_MODE = FileHandler.AutoMode.On;

    public JPBCODatabaseDirectory() {
        super(COMMAND_IDENTIFIERS, EXISTENCE_HANDLING, AUTO_MODE);
        registerDependingProperty(JPBCOVarDirectory.class);
        registerDependingProperty(JPVarDirectory.class);
        registerDependingProperty(JPShareDirectory.class);
    }

    @Override
    public File getParentDirectory() throws JPServiceException {
        try {
            final File bcoVar = JPService.getProperty(JPBCOVarDirectory.class).getValue();
            if (JPService.testMode() || bcoVar.exists() && new File(bcoVar, DEFAULT_DB_PATH).exists()) {
                return bcoVar;
            }
        } catch (JPNotAvailableException ex) {
            // continue with next location
        }

        try {
            final File prefixVar = new File(JPService.getProperty(JPVarDirectory.class).getValue(), "bco");
            if (prefixVar.exists() && new File(prefixVar, DEFAULT_DB_PATH).exists()) {
                return prefixVar;
            }
        } catch (JPNotAvailableException ex) {
            // continue with next location
        }

        try {
            final File prefixShare = new File(JPService.getProperty(JPShareDirectory.class).getValue(), "bco");
            if (prefixShare.exists() && new File(prefixShare, DEFAULT_DB_PATH).exists()) {
                return prefixShare;
            }
        } catch (JPNotAvailableException ex) {
            // continue with next location
        }


        // use bco var as default fallback where the database will be than generated during property validation.
        try {
            final File bcoVar = JPService.getProperty(JPBCOVarDirectory.class).getValue();
            if (bcoVar.exists()) {
                return bcoVar;
            }
        } catch (JPNotAvailableException ex) {
            // continue with next location
        }

        throw new JPServiceException("Could not auto detect database location!");
    }

    public static final String GIT_IGNORE_TEMPLATE = "### This file is auto generated and maintained by BCO ### \n\n" +
            "activity-template-db\n" +
            "agent-class-db\n" +
            "app-class-db\n" +
            "device-class-db\n" +
            "gateway-class-db\n" +
            "service-template-db\n" +
            "unit-template-db";

    @Override
    public void validate() throws JPValidationException {
        super.validate();

        // do nothing in case directory does not exist
        if (!getValue().exists()) {
            return;
        }

        final File gitIgroreFile = new File(getValue(), ".gitignore");

        // create file if not exist, otherwise validate
        if (!gitIgroreFile.exists()) {
            logger.info("Create new git ignore file to exclude external database.");

            try {
                FileUtils.writeStringToFile(gitIgroreFile, GIT_IGNORE_TEMPLATE, StandardCharsets.UTF_8, false);
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(new JPValidationException("Could not create git ignore file to exclude external databases to be committed.", ex), logger, LogLevel.WARN);
            }
        } else {
            // validate existing one
            try {
                final String fileContent = FileUtils.readFileToString(gitIgroreFile, StandardCharsets.UTF_8);
                if (!fileContent.equals(GIT_IGNORE_TEMPLATE)) {

                    logger.info("Recover git ignore file to exclude external database.");
                    try {
                    // recover file content
                    FileUtils.writeStringToFile(gitIgroreFile, GIT_IGNORE_TEMPLATE, StandardCharsets.UTF_8, false);
                    } catch (IOException ex) {
                        ExceptionPrinter.printHistory(new JPValidationException("Could not recover git ignore file content!", ex), logger, LogLevel.WARN);
                    }
                }
            } catch (IOException ex) {
                ExceptionPrinter.printHistory(new JPValidationException("Could validate git ignore file content!", ex), logger, LogLevel.WARN);
            }
        }
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File(DEFAULT_DB_PATH);
    }

    @Override
    public String getDescription() {
        return "Specifies the bco database directory. If not already exist, this database directory is auto generated from provided templates during startup.";
    }
}
