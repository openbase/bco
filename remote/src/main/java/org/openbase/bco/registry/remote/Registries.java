package org.openbase.bco.registry.remote;

/*
 * #%L
 * BCO Registry Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.ArrayList;
import java.util.List;
import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.app.remote.AppRegistryRemote;
import org.openbase.bco.registry.app.remote.CachedAppRegistryRemote;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.scene.remote.CachedSceneRegistryRemote;
import org.openbase.bco.registry.scene.remote.SceneRegistryRemote;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.bco.registry.user.remote.CachedUserRegistryRemote;
import org.openbase.bco.registry.user.remote.UserRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.storage.registry.RegistryRemote;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class Registries {

    /**
     * Returns a list of all available bco registries.
     *
     * @return a list of remote registry instances.
     * @throws CouldNotPerformException is throw if at least one registry is not available.
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static List<RegistryRemote> getRegistries(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        final List<RegistryRemote> registryList = new ArrayList<>();
        registryList.add(getAgentRegistry(waitForData));
        registryList.add(getAppRegistry(waitForData));
        registryList.add(getDeviceRegistry(waitForData));
        registryList.add(getLocationRegistry(waitForData));
        registryList.add(getSceneRegistry(waitForData));
        registryList.add(getUnitRegistry(waitForData));
        registryList.add(getUserRegistry(waitForData));
        return registryList;
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static UnitRegistryRemote getUnitRegistry() throws NotAvailableException, InterruptedException {
        return CachedUnitRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static AgentRegistryRemote getAgentRegistry() throws NotAvailableException, InterruptedException {
        return CachedAgentRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static AppRegistryRemote getAppRegistry() throws NotAvailableException, InterruptedException {
        return CachedAppRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static DeviceRegistryRemote getDeviceRegistry() throws NotAvailableException, InterruptedException {
        return CachedDeviceRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static LocationRegistryRemote getLocationRegistry() throws NotAvailableException, InterruptedException {
        return CachedLocationRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static SceneRegistryRemote getSceneRegistry() throws NotAvailableException, InterruptedException {
        return CachedSceneRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static UserRegistryRemote getUserRegistry() throws NotAvailableException, InterruptedException {
        return CachedUserRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
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
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static AgentRegistryRemote getAgentRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedAgentRegistryRemote.getRegistry().waitForData();
        }
        return CachedAgentRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static AppRegistryRemote getAppRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedAppRegistryRemote.getRegistry().waitForData();
        }
        return CachedAppRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static DeviceRegistryRemote getDeviceRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedDeviceRegistryRemote.getRegistry().waitForData();
        }
        return CachedDeviceRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static LocationRegistryRemote getLocationRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedLocationRegistryRemote.getRegistry().waitForData();
        }
        return CachedLocationRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static SceneRegistryRemote getSceneRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedSceneRegistryRemote.getRegistry().waitForData();
        }
        return CachedSceneRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @param waitForData defines if this call should block until the registry data is available.
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static UserRegistryRemote getUserRegistry(final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            CachedUserRegistryRemote.getRegistry().waitForData();
        }
        return CachedUserRegistryRemote.getRegistry();
    }

    /**
     * Method shutdown all registry instances.
     *
     * Please use method with care!
     * Make sure no other instances are using the cached remote instances before shutdown.
     *
     * Note: This method takes only effect in unit tests, otherwise this call is ignored. During normal operation there is not need for a manual registry shutdown because each registry takes care of its shutdown.
     */
    public static void shutdown() {
        CachedUnitRegistryRemote.shutdown();
        CachedAgentRegistryRemote.shutdown();
        CachedAppRegistryRemote.shutdown();
        CachedDeviceRegistryRemote.shutdown();
        CachedLocationRegistryRemote.shutdown();
        CachedSceneRegistryRemote.shutdown();
        CachedUserRegistryRemote.shutdown();
    }

    /**
     * Method only returns if all available registries are synchronized.
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static void waitForData() throws CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.waitForData();
        CachedAgentRegistryRemote.waitForData();
        CachedAppRegistryRemote.waitForData();
        CachedDeviceRegistryRemote.waitForData();
        CachedLocationRegistryRemote.waitForData();
        CachedSceneRegistryRemote.waitForData();
        CachedUserRegistryRemote.waitForData();
    }

    public static boolean isDataAvailable() {
        try {
            return CachedUnitRegistryRemote.getRegistry().isDataAvailable()
                    && CachedAgentRegistryRemote.getRegistry().isDataAvailable()
                    && CachedAppRegistryRemote.getRegistry().isDataAvailable()
                    && CachedDeviceRegistryRemote.getRegistry().isDataAvailable()
                    && CachedLocationRegistryRemote.getRegistry().isDataAvailable()
                    && CachedSceneRegistryRemote.getRegistry().isDataAvailable()
                    && CachedUserRegistryRemote.getRegistry().isDataAvailable();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
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
        CachedUnitRegistryRemote.reinitialize();
        CachedAgentRegistryRemote.reinitialize();
        CachedAppRegistryRemote.reinitialize();
        CachedDeviceRegistryRemote.reinitialize();
        CachedLocationRegistryRemote.reinitialize();
        CachedSceneRegistryRemote.reinitialize();
        CachedUserRegistryRemote.reinitialize();
    }

    /**
     * Method blocks until all registries are not handling any tasks and are all consistent.
     *
     * Note: If you have just modified the registry this method can maybe return immediately if the task is not yet received by the registry controller. So you should prefer the futures of the modification methods for synchronization tasks.
     *
     * @throws InterruptedException is thrown in case the thread was externally interrupted.
     * @throws org.openbase.jul.exception.CouldNotPerformException is thrown if the wait could not be performed.
     */
    public static void waitUntilReady() throws InterruptedException, CouldNotPerformException {
        CachedUnitRegistryRemote.waitUntilReady();
        CachedAgentRegistryRemote.waitUntilReady();
        CachedAppRegistryRemote.waitUntilReady();
        CachedDeviceRegistryRemote.waitUntilReady();
        CachedLocationRegistryRemote.waitUntilReady();
        CachedSceneRegistryRemote.waitUntilReady();
        CachedUserRegistryRemote.waitUntilReady();
    }
}
