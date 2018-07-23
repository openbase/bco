package org.openbase.bco.authentication.lib;

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

import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.processing.FileProcessor;
import org.openbase.jul.processing.JSonObjectFileProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TokenStore {

    public static Logger LOGGER = LoggerFactory.getLogger(TokenStore.class);

    private final Map<String, String> userIdTokenMap;
    private final String filename;
    private final FileProcessor<Map> fileProcessor;

    private File tokenStoreFile;

    public TokenStore(final String filename) {
        this.filename = filename;
        this.userIdTokenMap = new HashMap<>();
        this.fileProcessor = new JSonObjectFileProcessor<>(Map.class);
    }

    public void init() throws InitializationException {
        try {
            this.tokenStoreFile = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), filename);
            this.loadStore();
            CredentialStore.protectFile(tokenStoreFile);
        } catch (CouldNotPerformException | JPNotAvailableException ex) {
            throw new InitializationException(CredentialStore.class, ex);
        }
    }

    /**
     * Loads the credentials from a protobuf JSON file.
     *
     * @throws CouldNotPerformException If the deserialization fails.
     */
    private void loadStore() throws CouldNotPerformException {
        // create empty store if not available
        if (!tokenStoreFile.exists()) {
            saveStore();
        }

        // clear existing entries.
        userIdTokenMap.clear();

        try {
            // load new ones out of the credential store.
            for (Object entry : fileProcessor.deserialize(tokenStoreFile).entrySet()) {
                final Entry<String, String> entryCasted = (Entry<String, String>) entry;
                userIdTokenMap.put(entryCasted.getKey(), entryCasted.getValue());
            }
        } catch (ClassCastException ex) {
            throw new CouldNotPerformException("Could not load token store from file[" + tokenStoreFile.getAbsolutePath() + "]");
        }
    }

    /**
     * Stores the credentials in a protobuf JSON file.
     */
    private void saveStore() {
        try {
            // save into store
            fileProcessor.serialize(userIdTokenMap, tokenStoreFile);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
        }
    }

    public String getToken(final String id) throws NotAvailableException {
        if (userIdTokenMap.containsKey(id)) {
            return userIdTokenMap.get(id);
        }
        throw new NotAvailableException("token for id[" + id + "]");
    }

    public String addToken(final String id, final String token) {
        return userIdTokenMap.put(id, token);
    }

    public Map<String, String> getEntryMap() {
        return Collections.unmodifiableMap(userIdTokenMap);
    }

    public boolean contains(final String id) {
        return userIdTokenMap.containsKey(id);
    }

    public void shutdown() {
        saveStore();
    }
}
