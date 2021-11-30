package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.processing.FileProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

/**
 * Abstract class for a protected store. Protected means that the file the data of this
 * store is serialized to will only grant the executing user read and write permissions.
 * Currently this store manages an internal map with strings as keys/ids and arbitrary
 * data types as values. This class also handles loading and saving of the store.
 *
 * @param <DT>  the internal data type handled by this store
 * @param <SDT> the data type the internal map is converted to/from for de-/serialization
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractProtectedStore<DT, SDT> implements Shutdownable {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, DT> map;
    private final FileProcessor<SDT> fileProcessor;

    private File storeFile;

    /**
     * Create a new protected store.
     *
     * @param fileProcessor the file processor used for de-/serialization of the store
     */
    public AbstractProtectedStore(final FileProcessor<SDT> fileProcessor) {
        this.fileProcessor = fileProcessor;
        this.map = new HashMap<>();
    }

    /**
     * Initialize the protected store. This is done by handling a file with the given name
     * in the credentials directory. If the file does not exist it will be created.
     * If it already exists its content is loaded into the internal map.
     * In both cases the file will be protected meaning that read and write permissions will only be set
     * for the executing user.
     *
     * @param filename the filename for the file in the credentials directory for the store
     *
     * @throws InitializationException if initialization fails, e.g. because the executing user does not have permissions
     *                                 to load the file
     */
    public void init(final String filename) throws InitializationException {
        try {
            storeFile = new File(JPService.getProperty(JPCredentialsDirectory.class).getValue(), filename);
            loadStore();
            protectFile(storeFile);
        } catch (CouldNotPerformException | JPNotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Load the internal map from the store file. If the store file does not exist it will be created.
     *
     * @throws CouldNotPerformException if the deserialization fails
     */
    private void loadStore() throws CouldNotPerformException {
        // create empty store if not available
        if (!storeFile.exists()) {
            saveStore();
        }

        // clear existing entries.
        map.clear();

        try {
            // load from file
            load(fileProcessor.deserialize(storeFile), map);
        } catch (ClassCastException ex) {
            throw new CouldNotPerformException("Could not load store from file[" + storeFile.getAbsolutePath() + "]");
        }
    }

    /**
     * Stores the internal map into the store file.
     */
    private void saveStore() {
        try {
            // do not save file if not yet initialized.
            if(storeFile == null) {
                return;
            }

            // save into file
            fileProcessor.serialize(save(map), storeFile);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }

    /**
     * Return whether the internal map is empty.
     *
     * @return true the internal map is empty
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Determines if there is an entry with given id.
     *
     * @param id the id to check
     *
     * @return true if existent, false otherwise
     */
    public boolean hasEntry(final String id) {
        return map.containsKey(id);
    }

    /**
     * Get the entry for an id.
     *
     * @param id the id checked
     *
     * @return the entry for the given id
     *
     * @throws NotAvailableException if no entry for the id exists
     */
    public DT getEntry(final String id) throws NotAvailableException {
        if (hasEntry(id)) {
            return map.get(id);
        }

        throw new NotAvailableException("Entry with key[" + id + "]");
    }

    /**
     * Removes an entry from the store if existent and save.
     *
     * @param id the id of the entry to remove
     */
    public void removeEntry(final String id) {
        if (this.hasEntry(id)) {
            this.map.remove(id);
        }
        this.saveStore();
    }

    /**
     * Add or replace a value in the store belonging to an id.
     * Afterwards the store is saved.
     *
     * @param id    the id for which an entry is added/replaced
     * @param value the new value for the id
     */
    public void addEntry(final String id, final DT value) {
        map.put(id, value);
        this.saveStore();
    }

    /**
     * Get the internal map.
     *
     * @return an unmodifiable version of the internal map
     */
    public Map<String, DT> getEntryMap() {
        return Collections.unmodifiableMap(map);
    }

    /**
     * Get the number of entries in this store.
     *
     * @return the number of entries saved in this store.
     */
    public int getSize() {
        return map.size();
    }

    /**
     * Shutdown the store by saving it.
     */
    @Override
    public void shutdown() {
        if (JPService.testMode()) {
            map.clear();
        }
        saveStore();
    }

    /**
     * Load all entries from the de-serialized data into the internal map.
     *
     * @param data        the data de-serialized from the store file
     * @param internalMap the internal map which has to be filled from that
     */
    protected abstract void load(final SDT data, final Map<String, DT> internalMap);

    /**
     * Save all entries by generating the data to be serialized from the internal map.
     *
     * @param internalMap the internal map
     *
     * @return the data to be serialized
     */
    protected abstract SDT save(final Map<String, DT> internalMap);

    /**
     * Sets the permissions to UNIX 600 so only the owner has permission to read and to write to this protected file.
     *
     * @param file the file whose permissions are updated
     *
     * @throws CouldNotPerformException is thrown if the file could not be protected.
     */
    public static void protectFile(final File file) throws CouldNotPerformException {
        try {


            // test is class is supported on android
            try {
                PosixFilePermission.class.getSimpleName();
            } catch (Throwable ex) {
                LoggerFactory.getLogger(AbstractProtectedStore.class).warn(ex.getClass().getSimpleName() + ": Credential store can not be protected because of missing api support!");
                return;
            }

            try {
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(file.toPath(), perms);
            } catch (UnsupportedOperationException ex) {
                // apply windows fallback
                if (!file.setReadable(true, true)
                        || !file.setWritable(true, true)
                        || !file.setExecutable(true, true)) {
                    throw new CouldNotPerformException("Could not protect " + file.getAbsolutePath(), ex);
                }
            }
        } catch (IOException ex) {
            throw new CouldNotPerformException("Could not protect " + file.getAbsolutePath(), ex);
        }
    }
}
