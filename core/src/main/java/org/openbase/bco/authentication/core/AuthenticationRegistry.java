package org.openbase.bco.authentication.core;

/*-
 * #%L
 * BCO Authentication Core
 * %%
 * Copyright (C) 2017 openbase.org
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.authentication.lib.jp.JPInitializeCredentials;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.processing.JSonObjectFileProcessor;
import org.slf4j.LoggerFactory;

/**
 * This class provides access to the storage of login credentials.
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class AuthenticationRegistry {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticationRegistry.class);

    private static final String FILENAME = "credentials.json";

    protected HashMap<String, byte[]> credentials;

    private final JSonObjectFileProcessor<HashMap> fileProcessor;
    private File file;

    public AuthenticationRegistry() {
        this.fileProcessor = new JSonObjectFileProcessor<>(HashMap.class);
    }

    public void init() throws InitializationException {
        try {
            this.file = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), FILENAME);
            this.load();
            this.setPermissions();
        } catch (JPNotAvailableException | CouldNotPerformException | IOException ex) {
            throw new InitializationException(AuthenticationRegistry.class, ex);
        }
    }

    /**
     * Get the encrypted login credentials for a given user.
     *
     * @param userId ID of the user whose credentials should be retrieved.
     * @return The encrypted credentials, if they could be found.
     * @throws NotAvailableException If the user does not exist in the credentials storage.
     */
    public byte[] getCredentials(String userId) throws NotAvailableException {
        if (!credentials.containsKey(userId)) {
            throw new NotAvailableException(userId);
        }

        return credentials.get(userId);
    }

    /**
     * Sets the login credentials for a given user. If there is already an entry in the storage for
     * this user, it will be replaced. Otherwise, a new entry will be created.
     *
     * @param userId ID of the user to modify.
     * @param credentials New encrypted credentials.
     */
    public void setCredentials(String userId, byte[] credentials) {
        this.credentials.put(userId, credentials);
        this.save();
    }

    /**
     * Loads the credentials from a JSON file.
     *
     * @throws CouldNotPerformException If the deserialization fails.
     */
    private void load() throws CouldNotPerformException {
        try {
            if (!file.exists() && JPService.getProperty(JPInitializeCredentials.class).getValue()) {
                credentials = new HashMap<>();
                save();
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Initialize credential property not available!", ex);
        }
        credentials = fileProcessor.deserialize(file);
    }

    /**
     * Stores the credentials in a JSON file.
     */
    private void save() {
        try {
            fileProcessor.serialize(credentials, file);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
        }
    }

    /**
     * Sets the permissions to UNIX 600.
     *
     * @throws IOException
     */
    private void setPermissions() throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);

        Files.setPosixFilePermissions(file.toPath(), perms);
    }
}
