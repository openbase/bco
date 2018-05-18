package org.openbase.bco.registry.remote;

/*
 * #%L
 * BCO Registry Remote
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

import org.openbase.bco.registry.activity.remote.ActivityRegistryRemote;
import org.openbase.bco.registry.activity.remote.CachedActivityRegistryRemote;
import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.clazz.remote.ClassRegistryRemote;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.bco.registry.template.remote.TemplateRegistryRemote;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.storage.registry.RegistryRemote;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class Registries {

    /**
     * Returns a list of all available bco registries.
     *
     * @param waitForData
     * @return a list of remote registry instances.
     * @throws CouldNotPerformException is throw if at least one registry is not available.
     * @throws InterruptedException     is thrown if thread is externally interrupted.
     */
    public static List<RegistryRemote> getRegistries(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        final List<RegistryRemote> registryList = new ArrayList<>();
        registryList.add(getTemplateRegistry(waitForData));
        registryList.add(getClassRegistry(waitForData));
        registryList.add(getActivityRegistry(waitForData));
        registryList.add(getUnitRegistry(waitForData));
        return registryList;
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     */
    public static UnitRegistryRemote getUnitRegistry() throws NotAvailableException {
        return CachedUnitRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     */
    public static ActivityRegistryRemote getActivityRegistry() throws NotAvailableException {
        return CachedActivityRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     */
    public static ClassRegistryRemote getClassRegistry() throws NotAvailableException {
        return CachedClassRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException  is thrown if thread is externally interrupted.
     */
    public static TemplateRegistryRemote getTemplateRegistry() throws NotAvailableException {
        return CachedTemplateRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException  is thrown if thread is externally interrupted.
     */
    public static UnitRegistryRemote getUnitRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedUnitRegistryRemote.getRegistry().waitForData();
        }
        return CachedUnitRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException  is thrown if thread is externally interrupted.
     */
    public static ActivityRegistryRemote getActivityRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedActivityRegistryRemote.getRegistry().waitForData();
        }
        return CachedActivityRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException  is thrown if thread is externally interrupted.
     */
    public static ClassRegistryRemote getClassRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedClassRegistryRemote.getRegistry().waitForData();
        }
        return CachedClassRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException  is thrown if thread is externally interrupted.
     */
    public static TemplateRegistryRemote getTemplateRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedTemplateRegistryRemote.getRegistry().waitForData();
        }
        return CachedTemplateRegistryRemote.getRegistry();
    }

    /**
     * Method shutdown all registry instances.
     * <p>
     * Please use method with care!
     * Make sure no other instances are using the cached remote instances before shutdown.
     * <p>
     * Note: This method takes only effect in unit tests, otherwise this call is ignored. During normal operation there is not need for a manual registry shutdown because each registry takes care of its shutdown.
     */
    public static void shutdown() {
        CachedUnitRegistryRemote.shutdown();
        CachedActivityRegistryRemote.shutdown();
        CachedClassRegistryRemote.shutdown();
        CachedTemplateRegistryRemote.shutdown();
    }

    /**
     * Method only returns if all available registries are synchronized.
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException     is thrown if thread is externally interrupted.
     */
    public static void waitForData() throws CouldNotPerformException, InterruptedException {
        CachedTemplateRegistryRemote.waitForData();
        CachedClassRegistryRemote.waitForData();
        CachedActivityRegistryRemote.waitForData();
        CachedUnitRegistryRemote.waitForData();
    }

    public static boolean isDataAvailable() {
        try {
            return CachedUnitRegistryRemote.getRegistry().isDataAvailable()
                    && CachedTemplateRegistryRemote.getRegistry().isDataAvailable()
                    && CachedClassRegistryRemote.getRegistry().isDataAvailable()
                    && CachedActivityRegistryRemote.getRegistry().isDataAvailable();
        } catch (NotAvailableException ex) {
            // at least one remote is not available.
        }
        return false;
    }

    /**
     * Method forces a resynchronization on all remote registries.
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    public static void reinitialize() throws CouldNotPerformException, InterruptedException {
        CachedTemplateRegistryRemote.reinitialize();
        CachedClassRegistryRemote.reinitialize();
        CachedActivityRegistryRemote.reinitialize();
        CachedUnitRegistryRemote.reinitialize();
    }

    /**
     * Method blocks until all registries are not handling any tasks and are all consistent.
     * <p>
     * Note: If you have just modified the registry this method can maybe return immediately if the task is not yet received by the registry controller. So you should prefer the futures of the modification methods for synchronization tasks.
     *
     * @throws InterruptedException                                is thrown in case the thread was externally interrupted.
     * @throws org.openbase.jul.exception.CouldNotPerformException is thrown if the wait could not be performed.
     */
    public static void waitUntilReady() throws InterruptedException, CouldNotPerformException {
        CachedTemplateRegistryRemote.waitUntilReady();
        CachedClassRegistryRemote.waitUntilReady();
        CachedActivityRegistryRemote.waitUntilReady();
        CachedUnitRegistryRemote.waitUntilReady();
    }
}
